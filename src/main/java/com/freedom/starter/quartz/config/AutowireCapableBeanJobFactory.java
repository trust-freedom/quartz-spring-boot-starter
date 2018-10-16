package com.freedom.starter.quartz.config;

import org.quartz.spi.TriggerFiredBundle;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.scheduling.quartz.SpringBeanJobFactory;
import org.springframework.util.Assert;

/**
 * JobFactory
 * spring-context-support下SpringBeanJobFactory的子类，负责自动装配job（成员变量，方法参数等）
 */
class AutowireCapableBeanJobFactory extends SpringBeanJobFactory {

	/**
	 * AutowireCapableBeanFactory是BeanFactory接口的扩展，通过它可以实现自动装配
	 * AutowireCapableBeanFactory并不适用于普通的应用程序代码，对于典型的用例，请坚持使用BeanFactory 或 ListableBeanFactory
	 * 其他框架的集成代码可以利用此接口来装配和填充Spring无法控制其生命周期的现有Bean实例
	 */
	private final AutowireCapableBeanFactory beanFactory;

	AutowireCapableBeanJobFactory(AutowireCapableBeanFactory beanFactory) {
		Assert.notNull(beanFactory, "Bean factory must not be null");
		this.beanFactory = beanFactory;
	}

	/**
	 * 创建job实例
	 * 1、调用父类SpringBeanJobFactory的createJobInstance()，SpringBeanJobFactory负责再调用父类AdaptableJobFactory创建job实例，使用从scheduler context, job data map和trigger data map中获取的属性填充job
	 * 2、调用AutowireCapableBeanFactory#autowireBean(jobInstance)，自动装配job实例中的成员变量、方法参数等需要spring自动装配的内容
	 * 3、调用job实例的初始化方法，如下：
	 *   invokeAwareMethods
	 *   applyBeanPostProcessorsBeforeInitialization
	 *   invokeInitMethods -- InitializingBean#afterPropertiesSet()
	 *   applyBeanPostProcessorsAfterInitialization
	 * 注意：
	 *   方法执行后只会自动装配job的成员变量、方法参数等需要spring自动装配的内容，而不会将job实例添加到spring容器中
	 * @param bundle
	 * @return
	 * @throws Exception
	 */
	@Override
	protected Object createJobInstance(TriggerFiredBundle bundle) throws Exception {
		Object jobInstance = super.createJobInstance(bundle);

		this.beanFactory.autowireBean(jobInstance);
		this.beanFactory.initializeBean(jobInstance, null);

		return jobInstance;
	}

}
