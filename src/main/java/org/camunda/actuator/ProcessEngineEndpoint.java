package org.camunda.actuator;

import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.camunda.bpm.engine.impl.persistence.deploy.DeploymentCache;
import org.camunda.bpm.engine.repository.ProcessDefinition;
import org.springframework.boot.actuate.endpoint.AbstractEndpoint;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.*;

/**
 * Registers a Boot Actuator endpoint that provides information on the
 * running process instance and renders BPMN diagrams of the deployed processes.
 *
 * @author Josh Long
 */
@ConfigurationProperties(prefix = "endpoints.camunda")
public class ProcessEngineEndpoint extends AbstractEndpoint<Map<String, Object>> {

    private final ProcessEngine processEngine;

    public ProcessEngineEndpoint(ProcessEngine processEngine) {
        super("camunda");
        this.processEngine = processEngine;
    }

    @Override
    public Map<String, Object> invoke() {

        Map<String, Object> metrics = new HashMap<String, Object>();

        // Process definitions
        metrics.put("processDefinitionCount", processEngine.getRepositoryService().createProcessDefinitionQuery().count());

        // List of all process definitions
        List<ProcessDefinition> processDefinitions = processEngine.getRepositoryService().createProcessDefinitionQuery().orderByProcessDefinitionKey().asc().list();
        List<String> processDefinitionKeys = new ArrayList<String>();
        for (ProcessDefinition processDefinition : processDefinitions) {
            processDefinitionKeys.add(processDefinition.getKey() + " (v" + processDefinition.getVersion() + ")");
        }
        metrics.put("deployedProcessDefinitions", processDefinitionKeys);

        // Process instances
        Map<String, Object> processInstanceCountMap = new HashMap<String, Object>();
        metrics.put("runningProcessInstanceCount", processInstanceCountMap);
        for (ProcessDefinition processDefinition : processDefinitions) {
            processInstanceCountMap.put(processDefinition.getKey() + " (v" + processDefinition.getVersion() + ")",
                    processEngine.getRuntimeService().createProcessInstanceQuery().processDefinitionId(processDefinition.getId()).count());
        }
        Map<String, Object> completedProcessInstanceCountMap = new HashMap<String, Object>();
        metrics.put("completedProcessInstanceCount", completedProcessInstanceCountMap);
        for (ProcessDefinition processDefinition : processDefinitions) {
            completedProcessInstanceCountMap.put(processDefinition.getKey() + " (v" + processDefinition.getVersion() + ")",
                    processEngine.getHistoryService().createHistoricProcessInstanceQuery().finished().processDefinitionId(processDefinition.getId()).count());
        }

        // Open tasks
        metrics.put("openTaskCount", processEngine.getTaskService().createTaskQuery().count());
        metrics.put("completedTaskCount", processEngine.getHistoryService().createHistoricTaskInstanceQuery().finished().count());


        // Tasks completed today
        metrics.put("completedTaskCountToday", processEngine.getHistoryService().createHistoricTaskInstanceQuery().finished().taskDueAfter(
                new Date(System.currentTimeMillis() - secondsForDays(1))).count());

        // Process steps
        metrics.put("completedActivities", processEngine.getHistoryService().createHistoricActivityInstanceQuery().finished().count());

        // Process definition cache
        DeploymentCache deploymentCache = ((ProcessEngineConfigurationImpl) processEngine.getProcessEngineConfiguration()).getDeploymentCache();
        if (deploymentCache instanceof DeploymentCache) {
            metrics.put("cachedProcessDefinitionCount", ((DeploymentCache) deploymentCache).getProcessDefinitionCache().size());
        }
        return metrics;
    }

    private long secondsForDays(int days) {
        int hour = 60 * 60 * 1000;
        int day = 24 * hour;
        return days * day;
    }
}