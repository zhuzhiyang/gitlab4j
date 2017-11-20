package org.gitlab4j.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import java.text.ParseException;
import java.util.Date;
import java.util.List;

import org.gitlab4j.api.GitLabApi.ApiVersion;
import org.gitlab4j.api.models.ImpersonationToken;
import org.gitlab4j.api.models.ImpersonationToken.Scope;
import org.gitlab4j.api.models.User;
import org.gitlab4j.api.models.Version;
import org.gitlab4j.api.utils.ISO8601;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
* In order for these tests to run you must set the following properties in test-gitlab4j.properties
 * 
 * TEST_HOST_URL
 * TEST_PRIVATE_TOKEN
 * TEST_USERNAME
 * 
 * If any of the above are NULL, all tests in this class will be skipped.
 *
 * TEST_SUDO_AS_USERNAME
 *
 * If this is null the sudo() tyests will be skipped.
 *
 */
public class TestUserApi {

    // The following needs to be set to your test repository
    private static final String TEST_HOST_URL;
    private static final String TEST_PRIVATE_TOKEN;
    private static final String TEST_USERNAME;
    private static final String TEST_SUDO_AS_USERNAME;
    static {
        TEST_HOST_URL = TestUtils.getProperty("TEST_HOST_URL");
        TEST_PRIVATE_TOKEN = TestUtils.getProperty("TEST_PRIVATE_TOKEN");
        TEST_USERNAME = TestUtils.getProperty("TEST_USERNAME");
        TEST_SUDO_AS_USERNAME = TestUtils.getProperty("TEST_SUDO_AS_USERNAME");
    }

    private static final String TEST_IMPERSONATION_TOKEN_NAME = "token1";

    private static GitLabApi gitLabApi;

    public TestUserApi() {
        super();
    }

    @BeforeClass
    public static void setup() {

        String problems = "";
        if (TEST_HOST_URL == null || TEST_HOST_URL.trim().length() == 0) {
            problems += "TEST_HOST_URL cannot be empty\n";
        }

        if (TEST_PRIVATE_TOKEN == null || TEST_PRIVATE_TOKEN.trim().length() == 0) {
            problems += "TEST_PRIVATE_TOKEN cannot be empty\n";
        }

        if (TEST_USERNAME == null || TEST_USERNAME.trim().length() == 0) {
            problems += "TEST_USER_NAME cannot be empty\n";
        }

        if (problems.isEmpty()) {
            gitLabApi = new GitLabApi(ApiVersion.V4, TEST_HOST_URL, TEST_PRIVATE_TOKEN);
        } else {
            System.err.print(problems);
        }
    }

    @Before
    public void beforeMethod() {
        assumeTrue(gitLabApi != null);
    }

    @Test
    public void testGetVersion() throws GitLabApiException {
        Version version = gitLabApi.getVersion();
        assertNotNull(version);
        System.out.format("version=%s, revision=%s%n", version.getVersion(), version.getRevision());
        assertNotNull(version.getVersion());
        assertNotNull(version.getRevision());
    }
    
    @Test
    public void testGetCurrentUser() throws GitLabApiException {
        User currentUser = gitLabApi.getUserApi().getCurrentUser();
        assertNotNull(currentUser);
        assertEquals(TEST_USERNAME, currentUser.getUsername());
    }

    @Test
    public void testLookupUser() throws GitLabApiException {
        User user = gitLabApi.getUserApi().getUser(TEST_USERNAME);
        assertNotNull(user);
        assertEquals(TEST_USERNAME, user.getUsername());
    }

    @Test
    public void testSudoAsUser() throws GitLabApiException {

        assumeTrue(TEST_SUDO_AS_USERNAME != null);

        try {

            gitLabApi.sudo(TEST_SUDO_AS_USERNAME);
            User user = gitLabApi.getUserApi().getCurrentUser();
            assertNotNull(user);
            assertEquals(TEST_SUDO_AS_USERNAME, user.getUsername());
            Integer sudoAsId = user.getId();

            gitLabApi.sudo(null);
            user = gitLabApi.getUserApi().getCurrentUser();
            assertNotNull(user);
            assertEquals(TEST_USERNAME, user.getUsername());

            gitLabApi.unsudo();
            assertNull(gitLabApi.getSudoAsId());

            gitLabApi.setSudoAsId(sudoAsId);
            user = gitLabApi.getUserApi().getCurrentUser();
            assertNotNull(user);
            assertEquals(sudoAsId, user.getId());
            assertEquals(TEST_SUDO_AS_USERNAME, user.getUsername());

        } finally {
            gitLabApi.unsudo();
        }
    }

    @Test
    public void testCreateImpersonationToken() throws GitLabApiException, ParseException {

        User user = gitLabApi.getUserApi().getCurrentUser();
        Scope[] scopes = {Scope.API, Scope.READ_USER};
 //       Date expiresAt = ISO8601.toDate("2018-01-01T00:00:00Z");
        ImpersonationToken token = gitLabApi.getUserApi().createImpersonationToken(user.getId(), TEST_IMPERSONATION_TOKEN_NAME, "", scopes);
        assertNotNull(token);
        assertNotNull(token.getId());
        assertEquals(TEST_IMPERSONATION_TOKEN_NAME, token.getName());
        assertEquals(2, token.getScopes().size());

        gitLabApi.getUserApi().revokeImpersonationToken(user.getId(), token.getId());
    }

    @Test
    public void testGetImpersonationTokens() throws GitLabApiException, ParseException {

        User user = gitLabApi.getUserApi().getCurrentUser();
        Scope[] scopes = {Scope.API, Scope.READ_USER};
 //       Date expiresAt = ISO8601.toDate("2018-01-01T00:00:00Z");
        ImpersonationToken createdToken = gitLabApi.getUserApi().createImpersonationToken(user.getId(), TEST_IMPERSONATION_TOKEN_NAME, "", scopes);
        assertNotNull(createdToken);

        ImpersonationToken token =  gitLabApi.getUserApi().getImpersonationToken(user.getId(), createdToken.getId());
        assertNotNull(token);
        assertEquals(createdToken.getId(), token.getId());
        assertEquals(TEST_IMPERSONATION_TOKEN_NAME, token.getName());
        assertEquals(createdToken.getExpiresAt(), token.getExpiresAt());

        List<ImpersonationToken> tokens = gitLabApi.getUserApi().getImpersonationTokens(user.getId());
        assertNotNull(tokens);
        assertTrue(tokens.size() > 0);

        gitLabApi.getUserApi().revokeImpersonationToken(user.getId(), createdToken.getId());
    }

    @Test
    public void testDeleteImpersonationTokens() throws GitLabApiException, ParseException {

        User user = gitLabApi.getUserApi().getCurrentUser();
        System.out.println(user.getUsername());
        Scope[] scopes = {Scope.API, Scope.READ_USER};
  //      Date expiresAt = ISO8601.toDate("2018-01-01T00:00:00Z");
        ImpersonationToken createdToken = gitLabApi.getUserApi().createImpersonationToken(user.getId(), TEST_IMPERSONATION_TOKEN_NAME, "", scopes);
        assertNotNull(createdToken);

        ImpersonationToken token =  gitLabApi.getUserApi().getImpersonationToken(user.getId(), createdToken.getId());
        assertNotNull(token);
        assertEquals(createdToken.getId(), token.getId());
        assertTrue(token.getActive());

        gitLabApi.getUserApi().revokeImpersonationToken(user.getId(), createdToken.getId());
        token =  gitLabApi.getUserApi().getImpersonationToken(user.getId(), createdToken.getId());
        assertFalse(token.getActive());
    }
}
