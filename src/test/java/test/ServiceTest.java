/*
 * Created on 2015年12月12日
 *
 */
package test;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.activiti.engine.HistoryService;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.history.HistoricProcessInstance;
import org.activiti.engine.history.HistoricTaskInstance;
import org.activiti.engine.history.HistoricVariableInstance;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.activiti.engine.test.ActivitiRule;
import org.activiti.engine.test.Deployment;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:activiti.cfg.xml")
public class ServiceTest {

	@Rule
	public ActivitiRule activitiRule = new ActivitiRule();

	@Autowired
	private RuntimeService runtimeService;

	@Autowired
	private HistoryService historyService;

	@Autowired
	private RepositoryService repositoryService;
	
	@Autowired
	private TaskService taskService;

	@Test
	@Deployment(resources = { "process/Service.bpmn" })
	public void testDeployment() {
		long definitionCount = repositoryService.createProcessDefinitionQuery().count();
		System.out.println("Number of process definitions: " + definitionCount);
		Assert.assertEquals(null, 1, definitionCount);
		
		Map<String, Object> form = Collections.singletonMap("processVar", (Object) "val");		
		ProcessInstance p = runtimeService.startProcessInstanceByKey("serviceProcess", form);
		Assert.assertNotNull(p.getId());
		
		// ended immediately
		// get historic process instance
		List<HistoricProcessInstance> histProcess = historyService.createHistoricProcessInstanceQuery().processInstanceId(p.getId()).list();
		Assert.assertEquals(1, histProcess.size());
		
		// in history
		HistoricProcessInstance historicProcessInstance = histProcess.get(0);
		Assert.assertNotNull(historicProcessInstance);		
		System.out.println(historicProcessInstance.getName());
		
		// get historic variable instances
		List<HistoricVariableInstance> histVars = historyService.createHistoricVariableInstanceQuery().processInstanceId(p.getId()).list();
		Assert.assertEquals(3, histVars.size());
		
		// in history
		HistoricVariableInstance histVar = histVars.get(0);
		Assert.assertNotNull(histVar);		
		Assert.assertEquals("processVar", histVar.getVariableName());
		
		// get historic task instances, only usertask1 (started, not ended)
		List<HistoricTaskInstance> histTasks = historyService.createHistoricTaskInstanceQuery().processInstanceId(p.getId()).list();
		Assert.assertEquals(1, histTasks.size());
		
		List<Task> tasks = taskService.createTaskQuery().active().list();
		Assert.assertEquals(1, tasks.size());
		Task task1 = tasks.get(0);
		taskService.claim(task1.getId(), "xyz");
		Assert.assertEquals("usertask1", task1.getTaskDefinitionKey());
		
		// complete the task (first)
		taskService.complete(task1.getId());
		
		// get historic task instances, usertask1 (started and ended) and usertask2 (started, not ended)
		histTasks = historyService.createHistoricTaskInstanceQuery().processInstanceId(p.getId()).list();
		Assert.assertEquals(2, histTasks.size());
		
		tasks = taskService.createTaskQuery().active().list();
		Assert.assertEquals(1, tasks.size());
		Task task2 = tasks.get(0);
		taskService.claim(task2.getId(), "abcdef");
		Assert.assertEquals("usertask2", task2.getTaskDefinitionKey());
		
		// complete the task (second)
		taskService.complete(task2.getId());
		
		// no more
		Assert.assertNull(runtimeService.createProcessInstanceQuery().processInstanceId(p.getId()).singleResult());
	}

}
