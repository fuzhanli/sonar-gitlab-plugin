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
package com.talanlabs.sonar.plugins.gitlab.freemarker;

import com.alibaba.fastjson.JSON;
import com.talanlabs.sonar.plugins.gitlab.models.ReportIssue;
import freemarker.template.TemplateBooleanModel;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateScalarModel;
import org.sonar.api.batch.rule.Severity;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

/**
 * 问题类型查询
 */
public abstract class AbstractIssuesTypeTemplateMethodModelEx implements TemplateMethodModelEx {

    private static final Logger LOG = Loggers.get(AbstractIssuesTypeTemplateMethodModelEx.class);

    private final List<ReportIssue> reportIssues;

    AbstractIssuesTypeTemplateMethodModelEx(List<ReportIssue> reportIssues) {
        super();

        this.reportIssues = Collections.unmodifiableList(new ArrayList<>(reportIssues));
    }

    @Override
    public Object exec(List arguments) throws TemplateModelException {
        if (arguments.isEmpty()) {
            return execEmptyArg();
        } else if (arguments.size() == 1) {
            return execOneArg(arguments.get(0));
        } else if (arguments.size() == 2) {
            return execTwoArg(arguments.get(0), arguments.get(1));
        }
        throw new TemplateModelException("Failed call accept 0, 1 or 2 args");
    }

    protected abstract Object exec(Stream<ReportIssue> stream);

    private Object execEmptyArg() {
        return exec(reportIssues.stream());
    }

    private Object execOneArg(Object arg) throws TemplateModelException {
        if (arg instanceof TemplateScalarModel) {
            String name = ((TemplateScalarModel) arg).getAsString();
            try {
                return exec(reportIssues.stream().filter(i -> isTypeEquals(i, name)));
            } catch (IllegalArgumentException e) {
                throw new TemplateModelException("Failed call 1 Type arg (BUG,MINOR,VULNERABILITY,CODE_SMELL,SECURITY_HOTSPOT)", e);
            }
        } else if (arg instanceof TemplateBooleanModel) {
            boolean r = ((TemplateBooleanModel) arg).getAsBoolean();
            return exec(reportIssues.stream().filter(i -> isSameReportedOnDiff(i, r)));
        }
        throw new TemplateModelException("Failed call accept boolean or Severity");
    }

    private Object execTwoArg(Object arg1, Object arg2) throws TemplateModelException {
        if (arg1 instanceof TemplateScalarModel && arg2 instanceof TemplateBooleanModel) {
            String name = ((TemplateScalarModel) arg1).getAsString();
            boolean r = ((TemplateBooleanModel) arg2).getAsBoolean();
            try {
                return exec(reportIssues.stream().filter(i -> isTypeEquals(i, name) && isSameReportedOnDiff(i, r)));
            } catch (IllegalArgumentException e) {
                throw new TemplateModelException("Failed call Type arg (BUG,MINOR,VULNERABILITY,CODE_SMELL,SECURITY_HOTSPOT)", e);
            }
        }
        throw new TemplateModelException("Failed call accept 2 args boolean or Severity");
    }

    private boolean isTypeEquals(ReportIssue reportIssue, String ruleType) {

        LOG.debug("reportIssue: {} \n ruleType={}",JSON.toJSONString(reportIssue) ,ruleType);
        if(reportIssue.getIssue().getRule() == null){
            return false;
        }
        return ruleType.equals(reportIssue.getIssue().getRule().getType());
    }

    private boolean isSameReportedOnDiff(ReportIssue reportIssue, boolean r) {
        return (r && reportIssue.isReportedOnDiff()) || (!r && !reportIssue.isReportedOnDiff());
    }
}
