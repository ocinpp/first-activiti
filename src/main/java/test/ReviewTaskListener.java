/*
 * Created on 2015年12月31日
 *
 */
package test;

import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.DelegateTask;
import org.activiti.engine.delegate.TaskListener;

public class ReviewTaskListener implements TaskListener {

	public void notify(DelegateTask delegateTask) {
		Boolean approve = (Boolean) delegateTask.getVariable("approve");
		DelegateExecution execution = delegateTask.getExecution();
		
		if (approve) {
			Integer approvalCount = (Integer) execution.getVariable("approvalCount");
			execution.setVariable("approvalCount", ++approvalCount);
		} else {
			Integer rejectCount = (Integer) execution.getVariable("rejectCount");
			execution.setVariable("rejectCount", ++rejectCount);
		}
	}

}