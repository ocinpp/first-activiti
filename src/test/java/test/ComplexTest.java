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
import org.activiti.engine.ProcessEngine;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.history.HistoricProcessInstance;
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

	@Test
	@Deployment(resources = { "process/Complex.bpmn" })
	public void testFlowNotFree() {
		System.out.println(repositoryService.createProcessDefinitionQuery().list());

		Map<String, Object> form = new HashMap<String, Object>();
		form.put("request", "Test Request");
		form.put("price", new Long("1"));

		ArrayList<String> nameList = new ArrayList<String>();
		nameList.add("gonzo");
		nameList.add("kermit");
		form.put("reviewers", nameList);
		form.put("reviewerCount", nameList.size());
		form.put("approvalCount", 0);
		form.put("rejectCount", 0);

		ProcessInstance i = runtimeService.startProcessInstanceByKey("complexProcess", form);
		Assert.assertEquals(1, taskService.createTaskQuery().processInstanceId(i.getId()).count());

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
		
		// process
		List<Task> tasks3 = taskService.createTaskQuery().taskCandidateGroup("sales").active().list();
		Assert.assertEquals(1, tasks3.size());
		Task task3 = tasks3.get(0);
		
		// claim the task		
		taskService.claim(task3.getId(), "kermit");
		Assert.assertEquals("Check by Sales", task3.getName());
		
		// in the personal task list of the one that claimed the task
		task3 = taskService.createTaskQuery().taskId(task3.getId()).singleResult();	
		Assert.assertEquals("kermit", task3.getAssignee());
		
		// complete the task
		taskService.complete(task3.getId());
		
		// check if task count is zero
		tasks3 = taskService.createTaskQuery().taskAssignee("kermit").active().list();
		Assert.assertEquals(0, tasks3.size());		
	}

}
