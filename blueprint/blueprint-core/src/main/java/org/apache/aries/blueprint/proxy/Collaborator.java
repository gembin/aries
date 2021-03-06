/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.aries.blueprint.proxy;

import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Stack;

import org.apache.aries.blueprint.Interceptor;
import org.apache.aries.proxy.InvocationHandlerWrapper;
import org.osgi.service.blueprint.reflect.ComponentMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A collaborator which ensures preInvoke and postInvoke occur before and after
 * method invocation
 */
public class Collaborator implements InvocationHandlerWrapper, Serializable {

    /** Serial version UID for this class */
    private static final long serialVersionUID = -58189302118314469L;

    private static final Logger LOGGER = LoggerFactory
            .getLogger(Collaborator.class);

    private transient List<Interceptor> interceptors = null;
    private transient ComponentMetadata cm = null;

    public Collaborator(ComponentMetadata cm, List<Interceptor> interceptors) {
        this.cm = cm;
        this.interceptors = interceptors;
    }

    /**
     * Invoke the preCall method on the interceptor
     * 
     * @param cm
     *            : component Metadata
     * @param m
     *            : method
     * @param parameters
     *            : method paramters
     * @throws Throwable
     */
    private void preCallInterceptor(List<Interceptor> interceptorList,
            ComponentMetadata cm, Method m, Object[] parameters,
            Stack<Collaborator.StackElement> calledInterceptors)
            throws Throwable {
        if ((interceptors != null) && !(interceptors.isEmpty())) {
            for (Interceptor im : interceptorList) {
                Collaborator.StackElement se = new StackElement(im);

                // should we do this before or after the preCall ?
                calledInterceptors.push(se);

                // allow exceptions to propagate
                se.setPreCallToken(im.preCall(cm, m, parameters));
            }
        }
    }

    public Object invoke(Object proxy, Method method, Object[] args, InvocationHandler target)
            throws Throwable {
        Object toReturn = null;
        
        Stack<Collaborator.StackElement> calledInterceptors = new Stack<Collaborator.StackElement>();
        boolean inInvoke = false;
        try {
            preCallInterceptor(interceptors, cm, method, args,
                    calledInterceptors);
            inInvoke = true;
            toReturn = target.invoke(proxy, method, args);
            inInvoke = false;
            postCallInterceptorWithReturn(cm, method, toReturn,
                    calledInterceptors);

        } catch (Throwable e) {
            // whether the the exception is an error is an application decision
            LOGGER.debug("invoke", e);

            // if we catch an exception we decide carefully which one to
            // throw onwards
            Throwable exceptionToRethrow = null;
            // if the exception came from a precall or postcall interceptor
            // we will rethrow it
            // after we cycle through the rest of the interceptors using
            // postCallInterceptorWithException
            if (!inInvoke) {
                exceptionToRethrow = e;
            }
            // if the exception didn't come from precall or postcall then it
            // came from invoke
            // we will rethrow this exception if it is not a runtime
            // exception
            else {
                if (!(e instanceof RuntimeException)) {
                    exceptionToRethrow = e;
                }
            }
            try {
                postCallInterceptorWithException(cm, method, e,
                        calledInterceptors);
            } catch (Exception f) {
                // we caught an exception from
                // postCallInterceptorWithException
                // logger.catching("invoke", f);
                // if we haven't already chosen an exception to rethrow then
                // we will throw this exception
                if (exceptionToRethrow == null) {
                    exceptionToRethrow = f;
                } else {
                  LOGGER.warn("Discarding post-call with interceptor exception", f);
                }
            }
            // if we made it this far without choosing an exception we
            // should throw e
            if (exceptionToRethrow == null) {
                exceptionToRethrow = e;
            } else if (exceptionToRethrow != e) {
              LOGGER.warn("Discarding initial exception", e);
            }
            throw exceptionToRethrow;
        }
        return toReturn;
    }

    /**
     * Called when the method is called and returned normally
     * 
     * @param cm
     *            : component metadata
     * @param method
     *            : method
     * @param returnType
     *            : return type
     * @throws Throwable
     */
    private void postCallInterceptorWithReturn(ComponentMetadata cm,
            Method method, Object returnType,
            Stack<Collaborator.StackElement> calledInterceptors)
            throws Throwable {

        while (!calledInterceptors.isEmpty()) {
            Collaborator.StackElement se = calledInterceptors.pop();
            try {
                se.interceptor.postCallWithReturn(cm, method, returnType, se
                        .getPreCallToken());
            } catch (Throwable t) {
                LOGGER.debug("postCallInterceptorWithReturn", t);
                // propagate this to invoke ... further interceptors will be
                // called via the postCallInterceptorWithException method
                throw t;
            }
        } // end while
    }

    /**
     * Called when the method is called and returned with an exception
     * 
     * @param cm
     *            : component metadata
     * @param method
     *            : method
     * @param exception
     *            : exception thrown
     */
    private void postCallInterceptorWithException(ComponentMetadata cm,
            Method method, Throwable exception,
            Stack<Collaborator.StackElement> calledInterceptors)
            throws Throwable {
        Throwable tobeRethrown = null;
        while (!calledInterceptors.isEmpty()) {
            Collaborator.StackElement se = calledInterceptors.pop();

            try {
                se.interceptor.postCallWithException(cm, method, exception, se
                        .getPreCallToken());
            } catch (Throwable t) {
                // log the exception
                LOGGER.debug("postCallInterceptorWithException", t);
                if (tobeRethrown == null) {
                    tobeRethrown = t;
                } else {
                  LOGGER.warn("Discarding post-call with interceptor exception", t);
                }
            }

        } // end while

        if (tobeRethrown != null)
            throw tobeRethrown;
    }

    // info to store on interceptor stack during invoke
    private static class StackElement {
        private final Interceptor interceptor;
        private Object preCallToken;

        private StackElement(Interceptor i) {
            interceptor = i;
        }

        private void setPreCallToken(Object preCallToken) {
            this.preCallToken = preCallToken;
        }

        private Object getPreCallToken() {
            return preCallToken;
        }

    }

}