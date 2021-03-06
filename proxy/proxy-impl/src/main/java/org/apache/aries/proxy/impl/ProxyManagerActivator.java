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
package org.apache.aries.proxy.impl;

import org.apache.aries.proxy.ProxyManager;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class ProxyManagerActivator implements BundleActivator 
{
  private static final boolean ASM_PROXY_SUPPORTED;
  private AbstractProxyManager managerService;
  
  static
  {
    boolean classProxy = false;
    try {
      // Try load load a asm class (to make sure it's actually available
      // then create the asm factory
      Class.forName("org.objectweb.asm.ClassVisitor");
      classProxy = true;
    } catch (Throwable t) {
    }
    
    ASM_PROXY_SUPPORTED = classProxy;
  }
  
  public void start(BundleContext context)
  {
    if (ASM_PROXY_SUPPORTED) {
      managerService = new AsmProxyManager();
    } else {
      managerService = new JdkProxyManager();
    }
    
    context.registerService(ProxyManager.class.getName(), managerService, null);
  }

  public void stop(BundleContext context)
  {
  }
}