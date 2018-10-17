package com.freedom.starter.quartz.config;

import org.quartz.Calendar;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.Trigger;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnSingleCandidate;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;
import java.util.Properties;

@Configuration
@ConditionalOnClass({ Scheduler.class, SchedulerFactoryBean.class })
@EnableConfigurationProperties(QuartzProperties.class)
@AutoConfigureAfter({ DataSourceAutoConfiguration.class })  //在数据源自动配置后再配置
public class QuartzAutoConfiguration {

    /** quartz配置文件 */
    private QuartzProperties quartzProperties;

    /** SchedulerFactoryBean自定义器集合 */
    private final List<SchedulerFactoryBeanCustomizer> customizers;

    /** JobDetail数组，可向Spring容器中注册JobDetail，会被添加此数组 */
    private final JobDetail[] jobDetails;

    /** Calendar Map，可向Spring容器中注册Calendar，会被添加此Map */
    private final Map<String, Calendar> calendars;

    /** Trigger数组，可向Spring容器中注册Trigger，会被添加此数组 */
    private final Trigger[] triggers;

    private final ApplicationContext applicationContext;

    private final String CLASSPATH_PROPERTY_PATH_PREFIX = "classpath:";
    private final String FILE_PROPERTY_PATH_PREFIX = "file:";


    /**
     * QuartzConfiguration构造方法，从Spring容器中获取参数对应bean
     * 非必须的bean使用ObjectProvider#getIfAvailable()避免报错
     * @param quartzProperties
     * @param customizers
     * @param jobDetails
     * @param calendars
     * @param triggers
     * @param applicationContext
     */
    public QuartzAutoConfiguration(QuartzProperties quartzProperties,
                                   ObjectProvider<List<SchedulerFactoryBeanCustomizer>> customizers,
                                   ObjectProvider<JobDetail[]> jobDetails,
                                   ObjectProvider<Map<String, Calendar>> calendars,
                                   ObjectProvider<Trigger[]> triggers,
                                   ApplicationContext applicationContext) {
        this.quartzProperties = quartzProperties;
        this.customizers = customizers.getIfAvailable();
        this.jobDetails = jobDetails.getIfAvailable();
        this.calendars = calendars.getIfAvailable();
        this.triggers = triggers.getIfAvailable();
        this.applicationContext = applicationContext;
    }


    /**
     * Quartz Scheduler
     * @return
     */
    @Bean
    @ConditionalOnMissingBean
    public SchedulerFactoryBean quartzScheduler() {
        SchedulerFactoryBean schedulerFactoryBean = new SchedulerFactoryBean();
        schedulerFactoryBean.setJobFactory(  //设置jobFactory
                new AutowireCapableBeanJobFactory(this.applicationContext.getAutowireCapableBeanFactory()));

        //如果propertyFilePath不为空，且以classpath:或file:开头，使用指定的quartz.properties
        if(!StringUtils.isEmpty(this.quartzProperties.getPropertyFilePath())){
            if(this.quartzProperties.getPropertyFilePath().startsWith(CLASSPATH_PROPERTY_PATH_PREFIX)){
                schedulerFactoryBean.setConfigLocation(
                        new ClassPathResource(cutPropertyFilePath(this.quartzProperties.getPropertyFilePath(),CLASSPATH_PROPERTY_PATH_PREFIX)));
            }
            else if(this.quartzProperties.getPropertyFilePath().startsWith(FILE_PROPERTY_PATH_PREFIX)){
                schedulerFactoryBean.setConfigLocation(new FileSystemResource(cutPropertyFilePath(this.quartzProperties.getPropertyFilePath(),FILE_PROPERTY_PATH_PREFIX)));
            }
        }
        //如果没找到propertyFilePath，查看spring.quartz.properties.*配置信息
        else if (!this.quartzProperties.getProperties().isEmpty()) {
            schedulerFactoryBean.setQuartzProperties(asProperties(this.quartzProperties.getProperties()));
        }

        if (this.jobDetails != null && this.jobDetails.length > 0) {
            schedulerFactoryBean.setJobDetails(this.jobDetails);
        }
        if (this.calendars != null && !this.calendars.isEmpty()) {
            schedulerFactoryBean.setCalendars(this.calendars);
        }
        if (this.triggers != null && this.triggers.length > 0) {
            schedulerFactoryBean.setTriggers(this.triggers);
        }

        //调用所有SchedulerFactoryBeanCustomizer的customize()方法
        customize(schedulerFactoryBean);

        return schedulerFactoryBean;
    }


    /**
     * 去掉PropertyFilePath配置中的classpath: 或 file:
     * @param originalPropertyFilePath
     * @param pathPrefix
     * @return
     */
    private String cutPropertyFilePath(String originalPropertyFilePath, String pathPrefix){
        return originalPropertyFilePath.substring(pathPrefix.length(), originalPropertyFilePath.length());
    }


    /**
     * jdbc store的SchedulerFactoryBeanCustomizer
     * 如果配置属性jobStoreType=JDBC，且Spring容器中只有一个Datasource
     */
    @Configuration
    @ConditionalOnSingleCandidate(DataSource.class)  //如果Spring容器中只有一个Datasource 或者 只有一个@Primary的Datasource
    static class JdbcStoreTypeConfiguration {

        @Bean
        public SchedulerFactoryBeanCustomizer dataSourceCustomizer(/*final*/ QuartzProperties quartzProperties,
                                                                    /*final*/ DataSource dataSource,
                                                                    @QuartzDataSource ObjectProvider<DataSource> quartzDataSource){
            //SchedulerFactoryBeanCustomizer dataSourceCustomizer = new SchedulerFactoryBeanCustomizer(){
            //    @Override
            //    public void customize(SchedulerFactoryBean schedulerFactoryBean) {
            //        if(JobStoreType.JDBC == quartzProperties.getJobStoreType()){
            //            schedulerFactoryBean.setDataSource(dataSource);
            //            //PlatformTransactionManager txManager = transactionManager.getIfUnique();
            //            //if (txManager != null) {
            //            //    schedulerFactoryBean.setTransactionManager(txManager);
            //            //}
            //        }
            //    }
            //};

            DataSourceSchedulerFactoryBeanCustomizer dataSourceCustomizer = new DataSourceSchedulerFactoryBeanCustomizer();
            dataSourceCustomizer.setQuartzProperties(quartzProperties);
            dataSourceCustomizer.setDataSource(dataSource);
            dataSourceCustomizer.setQuartzDataSource(quartzDataSource.getIfAvailable());

            return dataSourceCustomizer;
        }
    }


    /**
     * 转换为Properties
     * @param source
     * @return
     */
    private Properties asProperties(Map<String, String> source) {
        Properties properties = new Properties();
        properties.putAll(source);
        return properties;
    }

    /**
     * 调用所有SchedulerFactoryBeanCustomizer的customize()方法
     * @param schedulerFactoryBean
     */
    private void customize(SchedulerFactoryBean schedulerFactoryBean) {
        if (this.customizers != null) {
            for (SchedulerFactoryBeanCustomizer customizer : this.customizers) {
                customizer.customize(schedulerFactoryBean);
            }
        }
    }

}
