/*
 * Created on 2016年5月11日
 *
 */
package test;

import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.JavaDelegate;

public class SampleDelegateService implements JavaDelegate {

	public void execute(DelegateExecution execution) throws Exception {
		System.out.println("Testing here in SampleDelegateService");
		System.out.println("Variables (SampleDelegateService) b4: " + execution.getVariables());
		execution.setVariable("delegateResult", "passed");
		System.out.println("Variables (SampleDelegateService) after: " + execution.getVariables());
	}

}
