/*
 * Created on 2015年12月12日
 *
 */
package test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.activiti.engine.HistoryService;
import org.activiti.engine.IdentityService;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.history.HistoricProcessInstance;
import org.activiti.engine.history.HistoricTaskInstance;
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
public class ComplexTest {

	@Rule
	public ActivitiRule activitiRule = new ActivitiRule();

	@Autowired
	private RuntimeService runtimeService;

	@Autowired
	private HistoryService historyService;

	@Autowired
	private TaskService taskService;

	@Autowired
	private RepositoryService repositoryService;
	
	@Autowired
	private IdentityService identityService;

	@Test
	@Deployment(resources = { "process/Complex.bpmn" })
	public void testFlowNotFree() {
		System.out.println(repositoryService.createProcessDefinitionQuery().list());

		Map<String, Object> form = new HashMap<String, Object>();
		form.put("request", "Test Request");
		form.put("price", new BigDecimal("100.5"));

		ArrayList<String> nameList = new ArrayList<String>();
		nameList.add("gonzo");
		nameList.add("kermit");
		form.put("reviewers", nameList);
		form.put("reviewerCount", nameList.size());
		form.put("approvalCount", 0);
		form.put("rejectCount", 0);

		ProcessInstance i = null;
		
		// set activiti:initiator
		try {
			// for this particular thread
			identityService.setAuthenticatedUserId("hello");
			i = runtimeService.startProcessInstanceByKey("complexProcess", form);						
		} finally {
			identityService.setAuthenticatedUserId(null);
		}
		
		// check task
		Assert.assertEquals(1, taskService.createTaskQuery().processInstanceId(i.getId()).count());
		
		// check initiator
		Assert.assertEquals("hello", runtimeService.getVariable(i.getId(), "initiator"));

		// check process instance owned by "hello"
		List<ProcessInstance> procs = runtimeService.createProcessInstanceQuery().variableValueEquals("initiator", "hello").list();
		Assert.assertEquals(1, procs.size());
		
		// start review
		// review 1
		// Task task = taskService.createTaskQuery().singleResult();
		List<Task> tasks = taskService.createTaskQuery().taskAssignee("gonzo").active().list();
		Assert.assertEquals(1, tasks.size());
		Task task = tasks.get(0);
		Assert.assertEquals("Review", task.getName());
		Assert.assertEquals("gonzo", task.getAssignee());

		Map<String, Object> taskVariables = new HashMap<String, Object>();
		taskVariables.put("approve", true);
		taskService.addComment(task.getId(), task.getProcessInstanceId(), "First Reviewer Comment");
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
		taskVariables2.put("approve", true);
		taskService.addComment(task2.getId(), task2.getProcessInstanceId(), "Second Reviewer Comment");
		taskService.complete(task2.getId(), taskVariables2);
		
		// check if task count is zero
		tasks2 = taskService.createTaskQuery().taskAssignee("kermit").active().list();
		Assert.assertEquals(0, tasks2.size());
		// end review
		
		// Check by Sales
		List<Task> tasks3 = taskService.createTaskQuery().taskCandidateGroup("sales").active().list();
		Assert.assertEquals(1, tasks3.size());
		Task task3 = tasks3.get(0);
		
		// claim the task
		taskService.claim(task3.getId(), "kermit");
		Assert.assertEquals("Check by Sales", task3.getName());
		
		// no more task in group
		tasks3 = taskService.createTaskQuery().taskCandidateGroup("sales").active().list();
		Assert.assertEquals(0, tasks3.size());
		
		// in the personal task list of the one that claimed the task
		task3 = taskService.createTaskQuery().taskId(task3.getId()).singleResult();	
		Assert.assertEquals("kermit", task3.getAssignee());
		
		// complete the task
		taskService.complete(task3.getId());
		
		// check if task count is zero
		tasks3 = taskService.createTaskQuery().taskAssignee("kermit").active().list();
		Assert.assertEquals(0, tasks3.size());	
		
		// Check by Marketing
		List<Task> tasks4 = taskService.createTaskQuery().taskCandidateGroup("marketing").active().list();
		Assert.assertEquals(1, tasks4.size());
		Task task4 = tasks4.get(0);
		
		// claim the task
		taskService.claim(task4.getId(), "fozzie");
		Assert.assertEquals("Check by Marketing", task4.getName());
		
		// no more task in group
		tasks4 = taskService.createTaskQuery().taskCandidateGroup("marketing").active().list();
		Assert.assertEquals(0, tasks4.size());
		
		// in the personal task list of the one that claimed the task
		task4 = taskService.createTaskQuery().taskId(task4.getId()).singleResult();	
		Assert.assertEquals("fozzie", task4.getAssignee());
		
		// complete the task
		taskService.complete(task4.getId());
		
		// Confirm by Management
		List<Task> tasks5 = taskService.createTaskQuery().taskCandidateGroup("management").active().list();
		Assert.assertEquals(1, tasks5.size());
		Task task5 = tasks5.get(0);
		
		// claim the task
		taskService.claim(task5.getId(), "gonzo");
		Assert.assertEquals("Confirm by Management", task5.getName());
		
		// no more task in group
		tasks5 = taskService.createTaskQuery().taskCandidateGroup("marketing").active().list();
		Assert.assertEquals(0, tasks5.size());
		
		// in the personal task list of the one that claimed the task
		task5 = taskService.createTaskQuery().taskId(task5.getId()).singleResult();	
		Assert.assertEquals("gonzo", task5.getAssignee());
		
		// complete the task
		taskService.complete(task5.getId());
		
		// the process will not return true for isEnded since it will be in history (if enabled)
		// reference: https://forums.activiti.org/content/processinstanceisended-returns-false
		Assert.assertNull(runtimeService.createProcessInstanceQuery().processInstanceId(i.getId()).singleResult());
		
		// get historic process instance owned by "hello"
		List<HistoricProcessInstance> hists = historyService.createHistoricProcessInstanceQuery().variableValueEquals("initiator", "hello").list();
		Assert.assertEquals(1, hists.size());
		
		HistoricProcessInstance historicProcessInstance = hists.get(0);
		Assert.assertNotNull(historicProcessInstance);
		System.out.println("Process instance end time: " + historicProcessInstance.getEndTime());
		
		List<HistoricTaskInstance> histTasks = historyService.createHistoricTaskInstanceQuery().processInstanceId(historicProcessInstance.getId()).list();
		Assert.assertEquals(5, histTasks.size());		
		
	}

}
