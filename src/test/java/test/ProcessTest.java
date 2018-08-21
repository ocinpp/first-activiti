/*
 * Created on 2015年12月12日
 *
 */
package test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.activiti.engine.HistoryService;
import org.activiti.engine.ProcessEngine;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.history.HistoricProcessInstance;
import org.activiti.engine.repository.ProcessDefinition;
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
public class ProcessTest {

	@Rule
	public ActivitiRule activitiRule = new ActivitiRule();

	@Autowired
	private RuntimeService runtimeService;

	@Autowired
	private HistoryService historyService;

	@Autowired
	private TaskService taskService;

	@Autowired
	private ProcessEngine processEngine;

	@Autowired
	private RepositoryService repositoryService;

	@Test
	@Deployment(resources = { "process/HelloWorldProcess.bpmn" })
	public void testDeployment() {
		long definitionCount = repositoryService.createProcessDefinitionQuery().count();
		System.out.println("Number of process definitions: " + definitionCount);
		Assert.assertEquals(null, 1, definitionCount);

		// {processDefinitionKey}:{processDefinitionVersion}:{generated-id}
		System.out.println(repositoryService.createProcessDefinitionQuery().list());
		ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().processDefinitionName("HelloWorldProcess").singleResult();
		System.out.println("Process definition: " + processDefinition);
		Assert.assertNotNull(processDefinition);

		ProcessDefinition d = repositoryService.createProcessDefinitionQuery().processDefinitionKey("helloWorldProcess").latestVersion().singleResult();
		System.out.println(d);

		repositoryService.createDeployment().name("test.bar").addClasspathResource("process/HelloWorldProcess.bpmn").deploy();

		System.out.println(repositoryService.createProcessDefinitionQuery().list());

		d = repositoryService.createProcessDefinitionQuery().processDefinitionKey("helloWorldProcess").latestVersion().singleResult();
		System.out.println(d);
	}

	@Test
	@Deployment(resources = { "process/Review.bpmn" })
	public void testReviewProcess() {
		System.out.println(repositoryService.createProcessDefinitionQuery().list());

		Map<String, Object> form = new HashMap<String, Object>();
		form.put("request", "Test Request");

		ArrayList<String> nameList = new ArrayList<String>();
		nameList.add("gonzo");
		nameList.add("kermit");
		form.put("reviewers", nameList);

		ProcessInstance i = runtimeService.startProcessInstanceByKey("reviewProcess2", form);

		// runtimeService.setVariable(i.getId(), "reviewers", nameList);
		// runtimeService.setVariableLocal(i.getId(), "wf_approveCount", 0);
		
		Assert.assertEquals(1, taskService.createTaskQuery().processInstanceId(i.getId()).count());

		// review 1
		// Task task = taskService.createTaskQuery().singleResult();
		List<Task> tasks = taskService.createTaskQuery().taskAssignee("gonzo").active().list();
		Assert.assertEquals(1, tasks.size());
		Task task = tasks.get(0);
		Assert.assertEquals("Review", task.getName());
		Assert.assertEquals("gonzo", task.getAssignee());

		Map<String, Object> taskVariables = new HashMap<String, Object>();
		taskVariables.put("approve", "true");
		taskService.complete(task.getId(), taskVariables);

		// check if task count is zero
		tasks = taskService.createTaskQuery().taskAssignee("gonzo").active().list();
		Assert.assertEquals(0, tasks.size());
		
		// review 2
		List<Task> tasks2 = taskService.createTaskQuery().taskAssignee("kermit").active().list();
		Assert.assertEquals(1, tasks2.size());
		Task task2 = tasks2.get(0);
		Assert.assertEquals("Review", task2.getName());
		Assert.assertEquals("kermit", task2.getAssignee());
		
		Map<String, Object> taskVariables2 = new HashMap<String, Object>();
		taskVariables2.put("approve", "true");
		taskService.complete(task2.getId(), taskVariables2);
		
		// check if task count is zero
		tasks2 = taskService.createTaskQuery().taskAssignee("kermit").active().list();
		Assert.assertEquals(0, tasks2.size());
		
		// process
		List<Task> tasks3 = taskService.createTaskQuery().taskCandidateGroup("management").active().list();
		Assert.assertEquals(1, tasks3.size());
		Task task3 = tasks3.get(0);
		
		// claim the task		
		taskService.claim(task3.getId(), "kermit");
		Assert.assertEquals("Process", task3.getName());
		
		// in the personal task list of the one that claimed the task
		task3 = taskService.createTaskQuery().taskId(task3.getId()).singleResult();	
		Assert.assertEquals("kermit", task3.getAssignee());
		
		// complete the task
		taskService.complete(task3.getId());
		
		// check if task count is zero
		tasks3 = taskService.createTaskQuery().taskAssignee("kermit").active().list();
		Assert.assertEquals(0, tasks3.size());

		// the process will not return true for isEnded since it will be in history (if enabled)
		// reference: https://forums.activiti.org/content/processinstanceisended-returns-false
		Assert.assertNull(runtimeService.createProcessInstanceQuery().processInstanceId(i.getId()).singleResult());
		
		HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery().processInstanceId(i.getId()).singleResult();
		Assert.assertNotNull(historicProcessInstance);
		System.out.println("Process instance end time: " + historicProcessInstance.getEndTime());
	}

	@Test
	@Deployment(resources = { "process/Review.bpmn" })
	public void testSuspendProcess() {
		System.out.println(repositoryService.createProcessDefinitionQuery().list());

		Map<String, Object> form = new HashMap<String, Object>();
		form.put("request", "Test Request");

		ArrayList<String> nameList = new ArrayList<String>();
		nameList.add("gonzo");
		nameList.add("kermit");
		form.put("reviewers", nameList);

		ProcessInstance i = runtimeService.startProcessInstanceByKey("reviewProcess2", form);
		runtimeService.suspendProcessInstanceById(i.getId());
		
		// active tasks
		List<Task> tasks = taskService.createTaskQuery().taskAssignee("gonzo").active().list();
		Assert.assertTrue(tasks.isEmpty());
		
		// all tasks
		tasks = taskService.createTaskQuery().taskAssignee("gonzo").list();
		Task task = tasks.get(0);
		Assert.assertTrue(task.isSuspended());

		// get process instance
		i = runtimeService.createProcessInstanceQuery().processInstanceId(task.getProcessInstanceId()).singleResult();
		Assert.assertTrue(i.isSuspended());
		
		// activate process instance
		runtimeService.activateProcessInstanceById(i.getId());
		i = runtimeService.createProcessInstanceQuery().processInstanceId(task.getProcessInstanceId()).singleResult();
		Assert.assertFalse(i.isSuspended());
	}
}
