package com.taboola.bitbucket;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.atlassian.bitbucket.repository.Branch;
import com.atlassian.bitbucket.repository.Repository;
import com.atlassian.bitbucket.repository.RepositoryService;
import com.atlassian.bitbucket.rest.util.ResourcePatterns;
import com.atlassian.bitbucket.scm.MergeCommandParameters;
import com.atlassian.bitbucket.scm.MergeCommandParameters.Builder;
import com.atlassian.bitbucket.scm.git.command.GitCommand;
import com.atlassian.bitbucket.scm.git.command.GitExtendedCommandFactory;
import com.atlassian.bitbucket.user.ApplicationUser;
import com.atlassian.bitbucket.user.UserService;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.user.UserManager;
import com.atlassian.sal.api.user.UserProfile;

@Path(ResourcePatterns.REPOSITORY_URI + "/merge")
public class MergeEndpoint {

    private static final Logger log = LoggerFactory.getLogger(MergeEndpoint.class);

    private final GitExtendedCommandFactory extendedCommandFactory;
    private final UserService userService;
    private final UserManager userManager;
    private final RepositoryService repositoryService;

    @Autowired
    public MergeEndpoint(@ComponentImport GitExtendedCommandFactory extendedCommandFactory,
                                @ComponentImport UserService userService,
                                @ComponentImport UserManager userManager,
                                @ComponentImport RepositoryService repositoryService) {
        this.extendedCommandFactory = extendedCommandFactory;
        this.userService = userService;
        this.userManager = userManager;
        this.repositoryService = repositoryService;
    }


    @GET
    @Produces({MediaType.APPLICATION_JSON})
    public Response mergeRequest(@PathParam("projectKey") String projectKey,
                                 @PathParam("repositorySlug") String repositorySlug,
                                 @QueryParam("user") String user,
                                 @QueryParam("comment") String comment,
                                 @QueryParam("fromBranch") String fromBranch,
                                 @QueryParam("toBranch") String toBranch) {
        UserProfile remoteUser = userManager.getRemoteUser();
        Map<String, String> errorMap = new HashMap<>();
        if (remoteUser == null || !userManager.isSystemAdmin(remoteUser.getUserKey())) {
            errorMap.put("error", "User is not admin user, not allowed");
            return Response.serverError().entity(errorMap).build();
        }
        String errorMessage = merge(projectKey, repositorySlug, user, comment, fromBranch, toBranch);
        if (StringUtils.isEmpty(errorMessage)) {
            return Response.ok().build();
        } else {
            errorMap.put("error", errorMessage);
            return Response.serverError().entity(errorMap).build();
        }
    }


    private String merge(String project, String repo, String user, String comment, String fromBranch, String toBranch) {
        ApplicationUser applicationUser = userService.getUserByName(user);
        if (applicationUser == null) {
            return "Cannot find user with name " + user;
        }
        Repository repository = repositoryService.getBySlug(project, repo);
        if (repository == null) {
            return "Cannot find repository with project " + project +  " and repo " + repo;
        }
        try {
            MergeCommandParameters mergeCommandParameters = new Builder()
                    .author(applicationUser)
                    .fromBranch(fromBranch)
                    .toBranch(toBranch)
                    .message(comment)
                    .build();
            GitCommand<Branch> merge = extendedCommandFactory.merge(repository, mergeCommandParameters);
            merge.synchronous().call();
            return "";
        } catch (Exception ex) {
            log.error("Got exception when trying to run merge operation", ex);
            return ex.getMessage();
        }
    }
}
