package com.freedom.starter.quartz.config;

import org.springframework.scheduling.quartz.SchedulerFactoryBean;

/**
 * Quartz Scheduler的自定义器接口
 */
public interface SchedulerFactoryBeanCustomizer {

	/**
	 * 自定义SchedulerFactoryBean
	 */
	void customize(SchedulerFactoryBean schedulerFactoryBean);

}
