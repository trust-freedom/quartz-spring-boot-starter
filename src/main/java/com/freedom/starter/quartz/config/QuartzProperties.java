package com.freedom.starter.quartz.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;


@ConfigurationProperties("spring.quartz")
public class QuartzProperties {

	/**
	 * Quartz job存储类型，默认内存
	 */
	private JobStoreType jobStoreType = JobStoreType.MEMORY;

	/**
	 * quartz.properties配置文件的路径，以classpath: 或 file: 开头
	 */
	private String propertyFilePath;

	/**
	 * 其它quartz scheduler属性
	 */
	private final Map<String, String> properties = new HashMap<>();



	public JobStoreType getJobStoreType() {
		return this.jobStoreType;
	}
	public void setJobStoreType(JobStoreType jobStoreType) {
		this.jobStoreType = jobStoreType;
	}
	public Map<String, String> getProperties() {
		return this.properties;
	}
	public String getPropertyFilePath() {
		return propertyFilePath;
	}
	public void setPropertyFilePath(String propertyFilePath) {
		this.propertyFilePath = propertyFilePath;
	}


}
