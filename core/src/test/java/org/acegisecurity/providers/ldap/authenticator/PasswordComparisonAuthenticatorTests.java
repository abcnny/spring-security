/* Copyright 2004, 2005, 2006 Acegi Technology Pty Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.acegisecurity.providers.ldap.authenticator;

import org.acegisecurity.BadCredentialsException;

import org.acegisecurity.ldap.AbstractLdapIntegrationTests;
import org.acegisecurity.ldap.InitialDirContextFactory;

import org.acegisecurity.providers.encoding.PlaintextPasswordEncoder;

import org.acegisecurity.userdetails.UsernameNotFoundException;
import org.acegisecurity.userdetails.ldap.LdapUserDetails;
import org.acegisecurity.userdetails.ldap.LdapUserDetailsImpl;
import org.acegisecurity.userdetails.ldap.LdapUserDetailsMapper;


/**
 * Tests for {@link PasswordComparisonAuthenticator}.
 *
 * @author Luke Taylor
 * @version $Id$
 */
public class PasswordComparisonAuthenticatorTests extends AbstractLdapIntegrationTests {
    //~ Instance fields ================================================================================================

    private PasswordComparisonAuthenticator authenticator;

    //~ Methods ========================================================================================================

    public void onSetUp() throws Exception {
        super.onSetUp();
        authenticator = new PasswordComparisonAuthenticator((InitialDirContextFactory) getContextSource());
        authenticator.setUserDnPatterns(new String[] {"uid={0},ou=people"});
    }

    public void onTearDown() throws Exception {
        super.onTearDown();
        // com.sun.jndi.ldap.LdapPoolManager.showStats(System.out);
    }

    public void testAllAttributesAreRetrivedByDefault() {
        LdapUserDetails user = authenticator.authenticate("bob", "bobspassword");
        //System.out.println(user.getAttributes().toString());
        assertEquals("User should have 5 attributes", 5, user.getAttributes().size());
    }

    public void testFailedSearchGivesUserNotFoundException()
        throws Exception {
        authenticator = new PasswordComparisonAuthenticator((InitialDirContextFactory) getContextSource());
        assertTrue("User DN matches shouldn't be available", authenticator.getUserDns("Bob").isEmpty());
        authenticator.setUserSearch(new MockUserSearch(null));
        authenticator.afterPropertiesSet();

        try {
            authenticator.authenticate("Joe", "password");
            fail("Expected exception on failed user search");
        } catch (UsernameNotFoundException expected) {}
    }

    public void testLocalComparisonSucceedsWithShaEncodedPassword() {
        // Ben's password is SHA encoded
        authenticator.authenticate("ben", "benspassword");
    }

    public void testLocalPasswordComparisonFailsWithWrongPassword() {
        try {
            authenticator.authenticate("Bob", "wrongpassword");
            fail("Authentication should fail with wrong password.");
        } catch (BadCredentialsException expected) {}
    }


   public void testLdapPasswordCompareFailsWithWrongPassword() {
       // Don't retrieve the password
       authenticator.setUserAttributes(new String[] {"uid", "cn", "sn"});
       try {
           authenticator.authenticate("Bob", "wrongpassword");
           fail("Authentication should fail with wrong password.");
       } catch(BadCredentialsException expected) {
       }
   }

    public void testLocalPasswordComparisonSucceedsWithCorrectPassword() {
        LdapUserDetails user = authenticator.authenticate("bob", "bobspassword");
        // check username is retrieved.
        assertEquals("bob", user.getUsername());
        assertEquals("bobspassword", user.getPassword());
    }

    public void testMultipleDnPatternsWorkOk() {
        authenticator.setUserDnPatterns(new String[] {"uid={0},ou=nonexistent", "uid={0},ou=people"});
        authenticator.authenticate("Bob", "bobspassword");
    }

    public void testOnlySpecifiedAttributesAreRetrieved() throws Exception {
        authenticator.setUserAttributes(new String[] {"uid", "userPassword"});
        authenticator.setPasswordEncoder(new PlaintextPasswordEncoder());

        LdapUserDetails user = authenticator.authenticate("Bob", "bobspassword");
        assertEquals("Should have retrieved 2 attribute (uid, userPassword)", 2, user.getAttributes().size());
    }

   public void testLdapCompareSucceedsWithCorrectPassword() {
       // Don't retrieve the password
       authenticator.setUserAttributes(new String[] {"uid"});
       // Bob has a plaintext password.
       authenticator.setPasswordEncoder(new PlaintextPasswordEncoder());
       authenticator.authenticate("bob", "bobspassword");
   }

   public void testLdapCompareSucceedsWithShaEncodedPassword() {
       // Don't retrieve the password
       authenticator.setUserAttributes(new String[] {"uid"});
       authenticator.authenticate("ben", "benspassword");
   }

    public void testPasswordEncoderCantBeNull() {
        try {
            authenticator.setPasswordEncoder(null);
            fail("Password encoder can't be null");
        } catch (IllegalArgumentException expected) {}
    }

    public void testUseOfDifferentPasswordAttribute() {
        LdapUserDetailsMapper mapper = new LdapUserDetailsMapper();
        mapper.setPasswordAttributeName("uid");
        authenticator.setPasswordAttributeName("uid");
        authenticator.setUserDetailsMapper(mapper);

        LdapUserDetails bob = authenticator.authenticate("bob", "bob");
    }

   public void testLdapCompareWithDifferentPasswordAttributeSucceeds() {
       authenticator.setUserAttributes(new String[] {"uid"});
       authenticator.setPasswordEncoder(new PlaintextPasswordEncoder());
       authenticator.setPasswordAttributeName("cn");
       authenticator.authenticate("bob", "Bob Hamilton");
   }

    public void testWithUserSearch() {
        authenticator = new PasswordComparisonAuthenticator((InitialDirContextFactory) getContextSource());
        assertTrue("User DN matches shouldn't be available", authenticator.getUserDns("Bob").isEmpty());

        LdapUserDetailsImpl.Essence userEssence = new LdapUserDetailsImpl.Essence();
        userEssence.setDn("uid=Bob,ou=people,dc=acegisecurity,dc=org");
        userEssence.setPassword("bobspassword");

        authenticator.setUserSearch(new MockUserSearch(userEssence.createUserDetails()));
        authenticator.authenticate("ShouldntBeUsed", "bobspassword");
    }
}
