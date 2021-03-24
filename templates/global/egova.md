<#if qualityGate??>
### SonarQube代码质量检查结果: 
+ commit id: ${commitSHA}
[点此查看代码存在的问题](${sonarUrl}dashboard?id=${projectKey})

<#assign newIssueCount = issueCount() notReportedIssueCount = issueCount(false)>
<#assign hasInlineIssues = newIssueCount gt notReportedIssueCount extraIssuesTruncated = notReportedIssueCount gt maxGlobalIssues>
<#if newIssueCount == 0>
#### 一、SonarQube未报告任何问题。
<#else>
#### 一、SonarQube报告了${newIssueCount}个问题
    <#assign newIssuesBlocker = issueCount(BLOCKER) newIssuesCritical = issueCount(CRITICAL) newIssuesMajor = issueCount(MAJOR) newIssuesMinor = issueCount(MINOR) newIssuesInfo = issueCount(INFO)>
##### 按照等级统计如下:     
    <#if newIssuesBlocker gt 0>
* ${imageSeverity(BLOCKER)} 阻断(blocker): ${newIssuesBlocker} 
    </#if>
    <#if newIssuesCritical gt 0>
* ${imageSeverity(CRITICAL)} 严重(critical): ${newIssuesCritical} 
    </#if>
    <#if newIssuesMajor gt 0>
* ${imageSeverity(MAJOR)} 主要(major): ${newIssuesMajor} 
    </#if>
    <#if newIssuesMinor gt 0>
* ${imageSeverity(MINOR)} 次要(minor): ${newIssuesMinor} 
    </#if>
    <#if newIssuesInfo gt 0>
* ${imageSeverity(INFO)} 信息(info): ${newIssuesInfo} 
    </#if>

##### 按照类型统计如下:     
    <#assign bugsCount = issueTypeCount("BUG") 
             vulnerCount = issueTypeCount("VULNERABILITY")
             smellCount = issueTypeCount("CODE_SMELL")
            securityCount = issueTypeCount("SECURITY_HOTSPOT")>
    <#if bugsCount gt 0>
* ${imageRuleType("BUG")}BUG: ${bugsCount}
    </#if>
    <#if vulnerCount gt 0>
* ${imageRuleType("VULNERABILITY")}漏洞(VULNERABILITY): ${vulnerCount}
    </#if>
    <#if smellCount gt 0>
* ${imageRuleType("CODE_SMELL")}异味(CODE_SMELL): ${smellCount}
    </#if>
    <#if securityCount gt 0>
* ${imageRuleType("SECURITY_HOTSPOT")}安全热点(SECURITY_HOTSPOT): ${securityCount}
    </#if>



<#if bugsCount gt 0>
##### 前5个BUG
    <#assign reportedIssueCount = 0>
    <#list issuesByType("BUG") as issue>
       <#if reportedIssueCount < 5>
* ${printByImage(issue)}
       <#assign reportedIssueCount++>
       </#if>
    </#list>
</#if>

    <#if !disableIssuesInline && hasInlineIssues>
在gitlab中查看commit中的评论信息检查代码。
    </#if>
    
    <#if notReportedIssueCount gt 0>
        <#if !disableIssuesInline>
            <#if hasInlineIssues || extraIssuesTruncated>
                <#if notReportedIssueCount <= maxGlobalIssues>

##### 存在${notReportedIssueCount}个其他问题
                <#else>

##### 前${maxGlobalIssues}个其他问题
                </#if>
            </#if>
注：其他问题是由于不能作为行注释报告，所以汇总在这里。
        <#elseif extraIssuesTruncated>

##### 前${maxGlobalIssues}个其他问题
        </#if>

        <#assign reportedIssueCount = 0>
        <#list issues(false) as issue>
            <#if reportedIssueCount < maxGlobalIssues>
1. ${printByImage(issue)}
            </#if>
            <#assign reportedIssueCount++>
        </#list>
        <#if notReportedIssueCount gt maxGlobalIssues>
* ... ${notReportedIssueCount-maxGlobalIssues} more
        </#if>
    </#if>
</#if>


#### 二、sonar质量阈: 
<#list qualityGate.conditions() as condition>
<@c condition=condition/>

</#list>
</#if>


<#macro c condition>* <@mn metricName=condition.metricName/> : <@s status=condition.status/>(当前值: ${condition.actual}<#if condition.status == WARN> ${condition.symbol} ${condition.warning}</#if><#if condition.status == ERROR> ${condition.symbol} ${condition.error}</#if>)</#macro>

<#macro mn metricName><#if metricName == "Security Rating on New Code">新代码安全级别<#elseif metricName == "Reliability Rating on New Code">新代码可靠性级别	<#elseif metricName == "Maintainability Rating on New Code">新代码可维护性级别<#elseif metricName == "Coverage on New Code is failed">新代码覆盖率<#elseif metricName == "Duplicated Lines on New Code (%)">新代码重复行比率<#else>${metricName}</#if></#macro>

<#macro s status><#if status == OK>通过<#elseif status == WARN>警告<#elseif status == ERROR>失败<#else>未知</#if></#macro>
