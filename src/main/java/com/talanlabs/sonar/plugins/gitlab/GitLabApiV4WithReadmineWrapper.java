/*
 * SonarQube :: GitLab Plugin
 * Copyright (C) 2016-2017 Talanlabs
 * gabriel.allaigre@gmail.com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package com.talanlabs.sonar.plugins.gitlab;

import com.talanlabs.gitlab.api.Paged;
import com.talanlabs.gitlab.api.v4.GitLabAPI;
import com.talanlabs.gitlab.api.v4.GitlabMergeRequestDiff;
import com.talanlabs.gitlab.api.v4.models.GitlabPosition;
import com.talanlabs.gitlab.api.v4.models.commits.GitLabCommit;
import com.talanlabs.gitlab.api.v4.models.commits.GitLabCommitComments;
import com.talanlabs.gitlab.api.v4.models.commits.GitLabCommitDiff;
import com.talanlabs.gitlab.api.v4.models.discussion.GitlabDiscussion;
import com.talanlabs.gitlab.api.v4.models.projects.GitLabProject;
import com.talanlabs.gitlab.api.v4.models.users.GitLabUser;
import com.taskadapter.redmineapi.RedmineException;
import com.taskadapter.redmineapi.RedmineManager;
import com.taskadapter.redmineapi.RedmineManagerFactory;
import com.taskadapter.redmineapi.bean.Issue;
import org.pegdown.PegDownProcessor;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class GitLabApiV4WithReadmineWrapper extends GitLabApiV4Wrapper {

    private static final Logger LOG = Loggers.get(GitLabApiV4WithReadmineWrapper.class);

    private RedmineManager redmine;

    private static final PegDownProcessor pdp = new PegDownProcessor(Integer.MAX_VALUE);


    public GitLabApiV4WithReadmineWrapper(GitLabPluginConfiguration config) {
        super(config);
    }

    @Override
    public void init() {
        super.init();
        redmine = RedmineManagerFactory.createWithApiKey(config.redmineUrl(), config.redmineUserToken());
    }

    @Override
    public void addGlobalComment(String comment) {
        super.addGlobalComment(comment);
        LOG.info("启用redmine推送!");
        String html=pdp.markdownToHtml(comment);
        pushComment2Redmine(html);
    }

    /**
     * 获取提交中的issueid
     * @return
     */
    private List<Integer> getIssueIDs(){
        List<Integer> issueIDs=new ArrayList<>();
        try {
            String title=gitLabAPIV4.getGitLabAPICommits().getCommit(gitLabProject.getId(), super.getFirstCommitSHA()).getTitle();
            Pattern pattern = Pattern.compile("\\[([0-9]*)\\]");
            Matcher matcher = pattern.matcher(title);
            while (matcher.find()) {
                issueIDs.add(Integer.parseInt(matcher.group(1)));
            }
        } catch (Exception e) {
            LOG.error("获取commit中的issue id失败！",e);
        }
        return issueIDs;
    }

    private void pushComment2Redmine(String comment){
        List<Integer> issueIDs=getIssueIDs();
        for (Integer id : issueIDs){
            Issue issue= null;
            try {
                issue = redmine.getIssueManager().getIssueById(id);
            } catch (RedmineException e) {
                LOG.error("从redmine获取Issue失败,id={}！",id,e);
            }
            if(issue==null){
                LOG.error("找不到issue={}!",id);
                continue;
            }
            LOG.info("推送redmine: issue={}...",id);

            issue.setNotes(comment);
            try {
                issue.update();
            } catch (RedmineException e) {
                LOG.error("保存issue失败,id={}！",id,e);
            }
        }
    }
}
