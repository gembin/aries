/*  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.aries.transaction.itests;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.sql.SQLException;

import javax.transaction.RollbackException;
import javax.transaction.UserTransaction;

import org.apache.aries.transaction.test.TestBean;
import org.junit.Test;

public class SupportsTranAttributeTest extends AbstractIntegrationTest {
  
  @Test
  public void testSupports() throws Exception {
      TestBean bean = getOsgiService(TestBean.class, "(tranAttribute=Supports)", DEFAULT_TIMEOUT);
      UserTransaction tran = getOsgiService(UserTransaction.class);
      
      //Test with client transaction - the insert succeeds because the bean delegates to
      //another bean with a transaction strategy of Mandatory, and the user transaction
      //is delegated
      int initialRows = bean.countRows();
      
      tran.begin();
      bean.insertRow("testWithClientTran", 1, true);
      tran.commit();
      
      int finalRows = bean.countRows();
      assertTrue("Initial rows: " + initialRows + ", Final rows: " + finalRows, finalRows - initialRows == 1);
      
      //Test with client transaction and application exception - the user transaction is not rolled back
      initialRows = bean.countRows();
      
      tran.begin();
      bean.insertRow("testWithClientTranAndWithAppException", 1);
      
      try {
          bean.insertRow("testWithClientTranAndWithAppException", 2, new SQLException());
      } catch (SQLException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
      }
      
      tran.commit();
      
      finalRows = bean.countRows();
      assertTrue("Initial rows: " + initialRows + ", Final rows: " + finalRows, finalRows - initialRows == 2);
      
      //Test with client transaction and runtime exception - the user transaction is rolled back
      initialRows = bean.countRows();
      
      tran.begin();
      bean.insertRow("testWithClientTranAndWithRuntimeException", 1);
      
      try {
          bean.insertRow("testWithClientTranAndWithRuntimeException", 2, new RuntimeException());
      } catch (RuntimeException e) {
          e.printStackTrace();
      }
      
      try {
          tran.commit();
          fail("RollbackException not thrown");
      } catch (RollbackException e) {
          e.printStackTrace();
      }
      
      finalRows = bean.countRows();
      assertTrue("Initial rows: " + initialRows + ", Final rows: " + finalRows, finalRows - initialRows == 0);
      
      //Test without client transaction - the insert fails because the bean delegates to
      //another bean with a transaction strategy of Mandatory, and no transaction is available
      initialRows = bean.countRows();
      
      try {
          bean.insertRow("testWithoutClientTran", 1, true);
          fail("Exception not thrown");
      } catch (Exception e) {
          e.printStackTrace();
      }
      
      finalRows = bean.countRows();
      assertTrue("Initial rows: " + initialRows + ", Final rows: " + finalRows, finalRows - initialRows == 0);
  }
}
