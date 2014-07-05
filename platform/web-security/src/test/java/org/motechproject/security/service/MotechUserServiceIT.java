package org.motechproject.security.service;

import org.ektorp.CouchDbConnector;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.motechproject.security.authentication.MotechPasswordEncoder;
import org.motechproject.security.domain.MotechRoleImpl;
import org.motechproject.security.domain.MotechUser;
import org.motechproject.security.repository.AllMotechRoles;
import org.motechproject.security.repository.AllMotechRolesImpl;
import org.motechproject.security.repository.AllMotechUsers;
import org.motechproject.security.repository.AllMotechUsersImpl;
import org.motechproject.testing.utils.SpringIntegrationTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.motechproject.security.UserRoleNames.USER_ADMIN_ROLE;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath*:/META-INF/motech/*.xml", "classpath*:/META-INF/security/*.xml"})
public class MotechUserServiceIT extends SpringIntegrationTest {

    @Autowired
    private AllMotechRoles allMotechRoles;

    @Autowired
    private MotechUserService motechUserService;

    @Autowired
    private AllMotechUsers allMotechUsers;

    @Autowired
    @Qualifier("webSecurityDbConnector")
    private CouchDbConnector connector;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private MotechPasswordEncoder passwordEncoder;

    @Before
    public void onStartUp() {
        ((AllMotechUsersImpl) allMotechUsers).removeAll();
        ((AllMotechRolesImpl) allMotechRoles).removeAll();
        // authorize
        allMotechRoles.add(new MotechRoleImpl("IT_ADMIN", asList("addUser", "editUser", "deleteUser", "manageUser", "activateUser", "manageRole"), false));
        motechUserService.register("admin", "admin", "admin@mail.com", "", asList("IT_ADMIN"), Locale.ENGLISH);
        UsernamePasswordAuthenticationToken authRequest = new UsernamePasswordAuthenticationToken("admin", "admin");
        Authentication auth = authenticationManager.authenticate(authRequest);
        SecurityContext context = SecurityContextHolder.getContext();
        context.setAuthentication(auth);
    }

    @Test
    public void testRegister() {
        motechUserService.register("userName", "password", "1234", "", asList("IT_ADMIN", "DB_ADMIN"), Locale.ENGLISH);
        MotechUser motechUser = allMotechUsers.findByUserName("userName");
        assertNotNull(motechUser);
        assertTrue(motechUser.getRoles().contains("IT_ADMIN"));
        assertTrue(motechUser.getRoles().contains("DB_ADMIN"));
    }

    @Test
    public void shouldActivateUser() {
        motechUserService.register("userName", "password", "1234", "", asList("IT_ADMIN", "DB_ADMIN"), Locale.ENGLISH, false, "");
        motechUserService.activateUser("userName");
        MotechUser motechUser = allMotechUsers.findByUserName("userName");
        assertTrue(motechUser.isActive());
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowExceptionIfUserNameIsEmptyForRegister() {
        motechUserService.register("", "password", "ext_id", "", new ArrayList<String>(), Locale.ENGLISH);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowExceptionIfUserNameIsEmptyForRegisterWithActiveInfo() {
        motechUserService.register("", "password", "ext_id", "", new ArrayList<String>(), Locale.ENGLISH, true, "");
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowExceptionIfPasswordIsEmptyForRegister() {
        motechUserService.register("user", "", "ext_id", "", new ArrayList<String>(), Locale.ENGLISH);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowExceptionIfUserNameisNull() {
        motechUserService.register(null, "", "ext_id", "", new ArrayList<String>(), Locale.ENGLISH);
    }

    @Test
    public void shouldNotActivateInvalidUser() {
        motechUserService.register("userName", "password", "1234", "", asList("IT_ADMIN", "DB_ADMIN"), Locale.ENGLISH, false, "");
        motechUserService.activateUser("userName1");
        MotechUser motechUser = allMotechUsers.findByUserName("userName");
        assertFalse(motechUser.isActive());
    }

    @Test
    public void shouldCreateActiveUserByDefault() {
        motechUserService.register("userName", "password", "1234", "", asList("IT_ADMIN", "DB_ADMIN"), Locale.ENGLISH);
        MotechUser motechUser = allMotechUsers.findByUserName("userName");
        assertTrue(motechUser.isActive());
    }

    @Test
    public void shouldCreateInActiveUser() {
        motechUserService.register("userName", "password", "1234", "", asList("IT_ADMIN", "DB_ADMIN"), Locale.ENGLISH, false, "");
        MotechUser motechUser = allMotechUsers.findByUserName("userName");
        assertFalse(motechUser.isActive());
    }

    @Test
    public void testPasswordEncoding() {
        String plainTextPassword = "testpassword";
        motechUserService.register("testuser", plainTextPassword, "entity1", "", asList("ADMIN"), Locale.ENGLISH);
        MotechUser motechUser = allMotechUsers.findByUserName("testuser");
        assertTrue(passwordEncoder.isPasswordValid(motechUser.getPassword(), plainTextPassword));
    }

    @Test
    public void shouldChangePassword() {
        motechUserService.register("userName", "password", "1234", "", asList("IT_ADMIN", "DB_ADMIN"), Locale.ENGLISH);
        motechUserService.changePassword("userName", "password", "newPassword");
        MotechUser motechUser = allMotechUsers.findByUserName("userName");
        assertTrue(passwordEncoder.isPasswordValid(motechUser.getPassword(), "newPassword"));
    }

    @Test
    public void shouldNotChangePasswordWithoutOldPassword() {
        motechUserService.register("userName", "password", "1234", "", asList("IT_ADMIN", "DB_ADMIN"), Locale.ENGLISH);
        motechUserService.changePassword("userName", "foo", "newPassword");
        MotechUser motechUser = allMotechUsers.findByUserName("userName");
        assertTrue(passwordEncoder.isPasswordValid(motechUser.getPassword(), "password"));
    }

    @Test
    public void hasUserShouldReturnTrueOnlyIfUserExists() {
        assertFalse(motechUserService.hasUser("username"));
        motechUserService.register("userName", "password", "1234", "", Arrays.asList("IT_ADMIN", "DB_ADMIN"), Locale.ENGLISH);
        assertTrue(motechUserService.hasUser("username"));
    }


    @Test
    public void shouldReturnPresenceOfAdminUser() {
        assertFalse(motechUserService.hasActiveAdminUser());
        motechUserService.register("adminUser", "password", "1234", "", asList(USER_ADMIN_ROLE), Locale.ENGLISH, true, "");
        assertTrue(motechUserService.hasActiveAdminUser());
    }

    @Test
    public void shouldValidateUserCredentials() {
        motechUserService.register("username", "password", "1234", "", asList("IT_ADMIN"), Locale.ENGLISH);
        assertNotNull(motechUserService.retrieveUserByCredentials("username", "password"));
        assertNull(motechUserService.retrieveUserByCredentials("username", "passw550rd"));
    }

    @Test
    public void shouldReturnEmptyListOfRolesForNonExistentUser() {
        List<String> roles = motechUserService.getRoles("non-existent");
        assertNotNull(roles);
        assertTrue(roles.isEmpty());
    }

    @After
    public void tearDown() {
        ((AllMotechUsersImpl) allMotechUsers).removeAll();
        ((AllMotechRolesImpl) allMotechRoles).removeAll();
        super.tearDown();
    }

    @Override
    public CouchDbConnector getDBConnector() {
        return connector;
    }
}