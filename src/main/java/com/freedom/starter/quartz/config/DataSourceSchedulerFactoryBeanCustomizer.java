package com.freedom.starter.quartz.config;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;

import javax.sql.DataSource;

public class DataSourceSchedulerFactoryBeanCustomizer implements SchedulerFactoryBeanCustomizer {

    private QuartzProperties quartzProperties;

    private DataSource dataSource;

    private DataSource quartzDataSource;


    public QuartzProperties getQuartzProperties() {
        return quartzProperties;
    }
    public void setQuartzProperties(QuartzProperties quartzProperties) {
        this.quartzProperties = quartzProperties;
    }
    public DataSource getDataSource() {
        return dataSource;
    }
    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }
    public DataSource getQuartzDataSource() {
        return quartzDataSource;
    }
    public void setQuartzDataSource(DataSource quartzDataSource) {
        this.quartzDataSource = quartzDataSource;
    }

    @Override
    public void customize(SchedulerFactoryBean schedulerFactoryBean) {
        if(JobStoreType.JDBC == quartzProperties.getJobStoreType()){
            schedulerFactoryBean.setDataSource(getDataSource(dataSource, quartzDataSource));
            //PlatformTransactionManager txManager = transactionManager.getIfUnique();
            //if (txManager != null) {
            //    schedulerFactoryBean.setTransactionManager(txManager);
            //}
        }
    }

    /**
     * 如果quartz指定了数据源quartzDataSource，就用指定的
     * 没有就用主数据源dataSource
     * @param dataSource
     * @param quartzDataSource
     * @return
     */
    private DataSource getDataSource(DataSource dataSource,
                                      DataSource quartzDataSource) {
        return (quartzDataSource != null) ? quartzDataSource : dataSource;
    }

}
