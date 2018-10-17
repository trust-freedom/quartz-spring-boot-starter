# quartz-spring-boot-starter

## 介绍

quartz-spring-boot-starter是一个Quartz定时任务框架的SpringBoot启动器，使SpringBoot 1.x可以方便的集成Quartz



## Quick Start

### 1、添加依赖

```xml
<dependency>
    <groupId>com.freedom</groupId>
    <artifactId>quartz-spring-boot-starter</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```



### 2、配置

Quartz框架的配置都集中在quartz.properties配置文件中，quartz-spring-boot-starter也沿用此思路，除了可以通过 quartz.properties这种Quartz框架专门的配置文件指定配置信息，也可以通过SpringBoot的application.properties或application.yml指定

#### application.properties方式

```properties
spring.quartz.properties.org.quartz.threadPool.class=org.quartz.simpl.SimpleThreadPool
spring.quartz.properties.org.quartz.threadPool.threadCount=10
spring.quartz.properties.org.quartz.threadPool.threadPriority=5
```

如上，以线程池配置为例，以`spring.quartz.properties.*`的方式添加Quartz框架的配置，配置项遵循Quartz官方



#### quartz.properties配置文件方式

```properties
#类路径下
spring.quartz.property-file-path=classpath:/quartz.properties

#文件绝对路径
spring.quartz.property-file-path=file:文件绝对路径
```

可以通过application.properties的配置项`spring.quartz.property-file-path`指定quartz.properties配置文件，有两种方式

- 指定类路径下文件，以 **classpath:** 开头
- 指定文件绝对路径，以 **file:** 开头

> **注意：** application.properties中的配置会覆盖quartz.properties中的配置



#### jobStoreType

另外添加一个针对job持久化类型的配置，在application.properties中

```properties
spring.quartz.jobStoreType=两种值：MEMORY 和 JDBC，默认值是MEMORY
```



### 3、Quartz数据源设置

如果`spring.quartz.jobStoreType=JDBC`，那么job数据会持久化到数据库中，使用的数据源一般有两种情况：

- 使用应用的唯一数据源 或 主数据源
- 使用为Quartz指定的数据源

如果使用应用的数据源，要么应用只有唯一的数据源，要么应用有多个数据源，但其中必须有且只有一个主数据源，可以通过 **@Primary** 指定

如果想指定Quartz自己的数据源，可以使用`@QuartzDataSource`注解指定数据源为Quartz数据源，会优先使用，如

**javaconfig方式：**

```java
@Bean
@QuartzDataSource  //指定是Quartz数据源
public DataSource myDataSource(){
    DruidDataSource datasource = new DruidDataSource();

    datasource.setDriverClassName("xxxx");
    datasource.setUrl("xxxx");
    datasource.setUsername("xxxx");
    datasource.setPassword("xxxx");
    ....

    return datasource;
}
```



**xml方式：**

```xml
<bean id="dataSource" class="com.alibaba.druid.pool.DruidDataSource">
    <property name="url" value="xxxx"/>
    <property name="username" value="xxxx"/>
    <property name="password" value="xxxx"/>
    ......
    <qualifier type="com.freedom.starter.quartz.config.QuartzDataSource" />  <!-- 指定是Quartz数据源 -->
</bean>
```



### 4、使用

**（1）正常启动后，通过@Autowired就可以获得`Scheduler`对象，操作quartz，如**

```java
@Autowired
Scheduler scheduler;

//创建Job
public void createJob(){
    //设置开始时间为30s后
    long startAtTime = System.currentTimeMillis() + 1000 * 30;
    //任务名称
    String name = UUID.randomUUID().toString();
    //任务所属分组
    String group = DemoJob.class.getName();
    
    //创建任务
    JobDetail jobDetail = JobBuilder.newJob(DemoJob.class).withIdentity(name,group).build();
    
    //创建任务触发器
    Trigger trigger = TriggerBuilder.newTrigger().withIdentity(name,group).startAt(new Date(startAtTime)).build();
    
    //将触发器与任务绑定到调度器内
    scheduler.scheduleJob(jobDetail, trigger);
}
```

DemoJob

```java
public class DemoJob extends QuartzJobBean {
    private static final Logger logger = LoggerFactory.getLogger(DemoJob.class);

    @Override
    protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
        logger.info("===========调用DemoJob#executeInternal()============");
    }

}
```



**（2）启动时加载Job**

如果jobStoreType=JDBC，那么启动时会加载数据库中的Job

还可以通过javaConfig的方式向Spring容器中注入实现了JobDetail 和 Trigger接口的实例，在创建ScheduleFactoryBean时会将Spring容器中的JobDetail 和 Trigger都添加进去



**（3）通过`SchedulerFactoryBeanCustomizer`自定义ScheduleFactoryBean**

如果目前对ScheduleFactoryBean的配置没有满足要求，可以实现`SchedulerFactoryBeanCustomizer`接口，其customize()方法会传入ScheduleFactoryBean对象，供用户自定义

```java
/**
 * Quartz Scheduler的自定义器接口
 */
public interface SchedulerFactoryBeanCustomizer {

	/**
	 * 自定义SchedulerFactoryBean
	 */
	void customize(SchedulerFactoryBean schedulerFactoryBean);

}
```



**（4）Job中使用Spring管理的bean**

可以在具体的QuartzJobBean中使用比如@Autowired等方式，Job在创建时会进行自动装配

> 由于JobFactory使用的是AutowireCapableBeanJobFactory，其中会使用AutowireCapableBeanFactory.autowireBean(jobInstance)对job实例的成员变量、方法参数等进行spring的自动装配



## FAQ

**1、为什么application.properties中的spring.quartz.properties.\*的配置会覆盖通过spring.quartz.property-file-path指定的quartz.properties配置文件中的配置？**

由于创建Scheduler对象使用的是spring-context-support包下的**SchedulerFactoryBean**，而在其`initSchedulerFactory()`方法中会创建一个合并后最终的Properties变量作为schedulerFactory初始化的属性，逻辑：

```java
Properties mergedProps = new Properties();

...
    
//quartz.properties配置文件位置不为空，加载配置文件内容到mergedProps
if (this.configLocation != null) {
	if (logger.isInfoEnabled()) {
		logger.info("Loading Quartz config from [" + this.configLocation + "]");
	}
	PropertiesLoaderUtils.fillProperties(mergedProps, this.configLocation);
}

//quartzProperties不为空，加载并合并配置项到mergedProps
CollectionUtils.mergePropertiesIntoMap(this.quartzProperties, mergedProps);
```

由于spring.quartz.property-file-path指定的quartz.properties配置文件会通过schedulerFactoryBean.setConfigLocation()设置为configLocation

而application.properties中的spring.quartz.properties.*会通过schedulerFactoryBean.setQuartzProperties()设置为quartzProperties

根据加载顺序以及合并逻辑，所以后者会覆盖前者



**2、在quartz.properties配置文件中已经可以指定`org.quartz.jobStore.class`，为什么还要单独新增一个starter的配置项`spring.quartz.jobStoreType`来指定持久化类型呢？**

同样是基于**SchedulerFactoryBean**中的`initSchedulerFactory()`逻辑：

```java
//StdSchedulerFactory.PROP_JOB_STORE_CLASS就是org.quartz.jobStore.class
//即如果SchedulerFactoryBean设置了dataSource，就会配置org.quartz.jobStore.class=org.springframework.scheduling.quartz.LocalDataSourceJobStore
if (this.dataSource != null) {
	mergedProps.put(StdSchedulerFactory.PROP_JOB_STORE_CLASS, LocalDataSourceJobStore.class.getName());
}
```

这段代码之前已经完成了**quartz.properties配置文件 **和 **quartzProperties配置项**的合并，即已经读取过了s用户的所有配置项，才有的这段逻辑

所以，只要SchedulerFactoryBean设置了dataSource，无论用户设置的org.quartz.jobStore.class是什么，都会使用LocalDataSourceJobStore

所以，通过新增的配置项`spring.quartz.jobStoreType`来判断，在创建SchedulerFactoryBean时是否要设置dataSource









