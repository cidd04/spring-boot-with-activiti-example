# Quickstart
## Preface
We forked and migrated this camunda project into Camunda

This document will describe how to get started with camunda and spring-boot. It is a port of the blog post [http://spring.io/blog/2015/03/08/getting-started-with-camunda-and-spring-boot](http://spring.io/blog/2015/03/08/getting-started-with-camunda-and-spring-boot). Minor tweaks to code and formatting have been done. Without further ado, the readme.

## Spring Boot integration
camunda and Spring play nicely together. The convention-over-configuration approach in Spring Boot works nicely with camunda�s process engine is setup and use. Out of the box, you only need a database, as process executions can span anywhere from a few seconds to a couple of years. Obviously, as an intrinsic part of a process definition is calling and consuming data to and from various systems with all kinds of technologies. The simplicity of adding the needed dependencies and integrating various pieces of (boiler-plate) logic with Spring Boot really makes this child�s play.

Using Spring Boot and camunda in a microservice approach also makes a lot of sense. Spring Boot makes it easy to get a production-ready service up and running in no time and - in a distributed microservice architecture - camunda processes can glue together various microservices while also weaving in human workflow (tasks and forms) to achieve a certain goal.

The Spring Boot integration in camunda was created by Spring expert [Josh Long](https://twitter.com/starbuxman). Josh and I [did a webinar a couple of months ago](https://www.youtube.com/watch?v=0PV_8Lew3vg) that should give you a good insight into the basics of the camunda integration for Spring Boot. The [camunda user guide section on Spring Boot](http://camunda.org/userguide/index.html#springSpringBoot) is also a great starting place to get more information.

## Getting Started

The code for this example can be [found in my Github repository](https://github.com/jbarrez/spring-boot-with-camunda-example).

The process we�ll implement here is a hiring process for a developer. It�s simplified of course (as it needs to fit on this web page), but you should get the core concepts. Here�s the diagram:

![Image of the hire process using BPMN2 symbolism](doc/images/hire-process.png)

As said in the introduction, all shapes here have a very specific interpretation thanks to the BPMN 2.0 standard. But even without knowledge of BPMN, the process is pretty easy to understand:

* When the process starts, the resume of the job applicant is stored in an external system.
* The process then waits until a telephone interview has been conducted. This is done by a user (see the little icon of a person in the corner).
* If the telephone interview wasn�t all that, a polite rejection email is sent. Otherwise, both a tech interview and financial negotiation should happen.
* Note that at any point, the applicant can cancel. That�s shown in the diagram as the event on the boundary of the big rectangle. When the event happens, everything inside will be killed and the process halts.
* If all goes well, a welcome email is sent.

This is the [BPMN for this process](https://github.com/jbarrez/spring-boot-with-camunda-example/blob/master/src/main/resources/processes/Developer_Hiring.bpmn20.xml)

Let�s create a new Maven project, and add the dependencies needed to get Spring Boot, camunda and a database. We�ll use an in memory database to keep things simple.

```maven
<dependency>
    <groupId>org.camunda.bpm.extension.springboot</groupId>
    <artifactId>camunda-bpm-spring-boot-starter-webapp</artifactId>
    <version>2.1.1</version>
</dependency>
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <version>1.4.185</version>
</dependency>
```

So only two dependencies is what is needed to create a very first Spring Boot + camunda application:

```java
@SpringBootApplication
public class MyApp {

    public static void main(String[] args) {
        SpringApplication.run(MyApp.class, args);
    } 
}
```

You could already run this application, it won�t do anything functionally but behind the scenes it already

* creates an in-memory H2 database
* creates an camunda process engine using that database
* exposes all camunda services as Spring Beans
* configures tidbits here and there such as the camunda async job executor, mail server, etc.

Let�s get something running. Drop the BPMN 2.0 process definition into the `src/main/resources/processes` folder. All processes placed here will automatically be deployed (ie. parsed and made to be executable) to the camunda engine. Let�s keep things simple to start, and create a `CommanLineRunner` that will be executed when the app boots up:

```java
@Bean
CommandLineRunner init( final RepositoryService repositoryService,
                              final RuntimeService runtimeService,
                              final TaskService taskService) {

    return new CommandLineRunner() {

        public void run(String... strings) throws Exception {
            Map<String, Object> variables = new HashMap<String, Object>();
            variables.put("applicantName", "John Doe");
            variables.put("email", "john.doe@camunda.com");
            variables.put("phoneNumber", "123456789");
            runtimeService.startProcessInstanceByKey("hireProcess", variables);
        }
    };
}
```

So what�s happening here is that we create a map of all the variables needed to run the process and pass it when starting process. If you�d check the process definition you�ll see we reference those variables using ${variableName} in many places (such as the task description).

The first step of the process is an automatic step (see the little cogwheel icon), implemented using an expression that uses a Spring Bean:

![Store resume part of the BPMN diagram](doc/images/servicetask.png)

which is implemented with

```
camunda:expression="${resumeService.storeResume()}"
```

Of course, we need that bean or the process would not start. So let�s create it:

```java
@Component
public class ResumeService {

    public void storeResume() {
        System.out.println("Storing resume ...");
    }

}
```

When running the application now, you�ll see that the bean is called:

```
  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/
 :: Spring Boot ::        (v1.2.0.RELEASE)

2015-02-16 11:55:11.129  INFO 304 --- [           main] MyApp                                    : Starting MyApp on The-camunda-Machine.local with PID 304 ...
Storing resume ...
2015-02-16 11:55:13.662  INFO 304 --- [           main] MyApp                                    : Started MyApp in 2.788 seconds (JVM running for 3.067)
```

And that�s it! Congrats with running your first process instance using camunda in Spring Boot!

Let�s spice things up a bit, and add following dependency to our pom.xml:

```maven
<dependency>
  <groupId>org.camunda.bpm.extension.springboot</groupId>
  <artifactId>camunda-bpm-spring-boot-starter-rest</artifactId>
  <version>2.1.1</version>
</dependency>
```

Having this on the classpath does a nifty thing: it takes the camunda REST API (which is written in Spring MVC) and exposes this fully in your application. The REST API of camunda [is fully documented in the camunda User Guide](http://camunda.org/userguide/index.html#_rest_api).

The REST API is secured by basic auth, and won�t have any users by default. Let�s add an admin user to the system as shown below (add this to the MyApp class). Don�t do this in a production system of course, there you�ll want to hook in the authentication to LDAP or something else.

```java
@Bean
InitializingBean usersAndGroupsInitializer(final IdentityService identityService) {

    return new InitializingBean() {
        public void afterPropertiesSet() throws Exception {

            Group group = identityService.newGroup("user");
            group.setName("users");
            group.setType("security-role");
            identityService.saveGroup(group);

            User admin = identityService.newUser("admin");
            admin.setPassword("admin");
            identityService.saveUser(admin);

        }
    };
}
```

Start the application. We can now start a process instance as we did in the `CommandLineRunner`, but now using REST:

```
curl -u admin:admin -H "Content-Type: application/json" -d '{"processDefinitionKey":"hireProcess", "variables": [ {"name":"applicantName", "value":"John Doe"}, {"name":"email", "value":"john.doe@alfresco.com"}, {"name":"phoneNumber", "value":"1234567"} ]}' http://localhost:8080/runtime/process-instances
Which returns us the json representation of the process instance:
```

```
{
     "tenantId": "",
     "url": "http://localhost:8080/runtime/process-instances/5",
     "activityId": "sid-42BAE58A-8FFB-4B02-AAED-E0D8EA5A7E39",
     "id": "5",
     "processDefinitionUrl": "http://localhost:8080/repository/process-definitions/hireProcess:1:4",
     "suspended": false,
     "completed": false,
     "ended": false,
     "businessKey": null,
     "variables": [],
     "processDefinitionId": "hireProcess:1:4"
}
```

I just want to stand still for a moment how cool this is. Just by adding one dependency, you�re getting the whole camunda REST API embedded in your application!

Let�s make it even cooler.
This adds a Spring Boot actuator endpoint for camunda. If we restart the application, and hit http://localhost:8080/camunda/, we get some basic stats about our processes. With some imagination that in a live system you�ve got many more process definitions deployed and executing, you can see how this is useful.

The same actuator is also registered as a JMX bean exposing similar information.

```
{
    completedTaskCountToday: 0,
    deployedProcessDefinitions: [
       "hireProcess (v1)"
    ],
    processDefinitionCount: 1,
   cachedProcessDefinitionCount: 1,
   runningProcessInstanceCount: {
       hireProcess (v1): 0
    },
    completedTaskCount: 0,
    completedcamundaes: 0,
    completedProcessInstanceCount: {
        hireProcess (v1): 0
    },
    openTaskCount: 0
}
```

To finish our coding, let�s create a dedicated REST endpoint for our hire process, that could be consumed by for example a javascript web application (out of scope for this article). So most likely, we�ll have a form for the applicant to fill in the details we�ve been passing programmatically above. And while we�re at it, let�s store the applicant information as a JPA entity. In that case, the data won�t be stored in camunda anymore, but in a separate table and referenced by camunda when needed.

You probably guessed it by now, JPA support is enabled by default

and add the entity to the MyApp class:

```java
@Entity
class Applicant {

    @Id
    @GeneratedValue
    private Long id;

    private String name;

    private String email;

    private String phoneNumber;

    // Getters and setters
```

We�ll also need a Repository for this Entity (put this in a separate file or also in MyApp). No need for any methods, the Repository magic from Spring will generate the methods we need for us.

```java
public interface ApplicantRepository extends JpaRepository<Applicant, Long> {
  // .. 
}
```

And now we can create the dedicated REST endpoint:

```java
@RestController
public class MyRestController {

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private ApplicantRepository applicantRepository;

    @RequestMapping(value="/start-hire-process", method= RequestMethod.POST, produces= MediaType.APPLICATION_JSON_VALUE)
    public void startHireProcess(@RequestBody Map<String, String> data) {

        Applicant applicant = new Applicant(data.get("name"), data.get("email"), data.get("phoneNumber"));
        applicantRepository.save(applicant);

        Map<String, Object> variables = new HashMap<String, Object>();
        variables.put("applicant", applicant);
        runtimeService.startProcessInstanceByKey("hireProcessWithJpa", variables);
    } 
}
```

Note we�re now using a slightly different process called `hireProcessWithJpa`, which has a few tweaks in it to cope with the fact the data is now in a JPA entity. So for example, we can�t use `${applicantName}` anymore, but we now have to use `${applicant.name}`.

Let�s restart the application and start a new process instance:

```
curl -u admin:admin -H "Content-Type: application/json" -d '{"name":"John Doe", "email": "john.doe@alfresco.com", "phoneNumber":"123456789"}' http://localhost:8080/start-hire-process
```

We can now go through our process. You could create a custom endpoints for this too, exposing different task queries with different forms � but I�ll leave this to your imagination and use the default camunda REST end points to walk through the process.

Let�s see which task the process instance currently is at (you could pass in more detailed parameters here, for example the `processInstanceId` for better filtering):

```
curl -u admin:admin -H "Content-Type: application/json" http://localhost:8080/runtime/tasks
```

which returns

```
{
     "order": "asc",
     "size": 1,
     "sort": "id",
     "total": 1,
     "data": [{
          "id": "14",
          "processInstanceId": "8",
          "createTime": "2015-02-16T13:11:26.078+01:00",
          "description": "Conduct a telephone interview with John Doe. Phone number = 123456789",
          "name": "Telephone interview"
          ...
     }],
     "start": 0
}
```

So, our process is now at the Telephone interview. In a realistic application, there would be a task list and a form that could be filled in to complete this task. Let�s complete this task (we have to set the `telephoneInterviewOutcome` variable as the exclusive gateway uses it to route the execution):

```
curl -u admin:admin -H "Content-Type: application/json" -d '{"action" : "complete", "variables": [ {"name":"telephoneInterviewOutcome", "value":true} ]}' http://localhost:8080/runtime/tasks/14
```

When we get the tasks again now, the process instance will have moved on to the two tasks in parallel in the subprocess (big rectangle):

```
{
     "order": "asc",
     "size": 2,
     "sort": "id",
     "total": 2,
     "data": [
          {
              ...
               "name": "Tech interview"
          },
          {
              ...
               "name": "Financial negotiation"
          }
     ],
     "start": 0
}
```

We can now continue the rest of the process in a similar fashion, but I�ll leave that to you to play around with.

## Testing

One of the strengths of using camunda for creating business processes is that everything is simply Java. As a consequence, processes can be tested as regular Java code with unit tests. Spring Boot makes writing such test a breeze.

Here�s how the unit test for the �happy path� looks like (while omitting `@Autowired` fields and test e-mail server setup). The code also shows the use of the camunda API�s for querying tasks for a given group and process instance.

```java
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = {MyApp.class})
@WebAppConfiguration
@IntegrationTest
public class HireProcessTest {

    @Test
    public void testHappyPath() {

        // Create test applicant
        Applicant applicant = new Applicant("John Doe", "john@camunda.org", "12344");
        applicantRepository.save(applicant);

        // Start process instance
        Map<String, Object> variables = new HashMap<String, Object>();
        variables.put("applicant", applicant);
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("hireProcessWithJpa", variables);

        // First, the 'phone interview' should be active
        Task task = taskService.createTaskQuery()
                .processInstanceId(processInstance.getId())
                .taskCandidateGroup("dev-managers")
                .singleResult();
        Assert.assertEquals("Telephone interview", task.getName());

        // Completing the phone interview with success should trigger two new tasks
        Map<String, Object> taskVariables = new HashMap<String, Object>();
        taskVariables.put("telephoneInterviewOutcome", true);
        taskService.complete(task.getId(), taskVariables);

        List<Task> tasks = taskService.createTaskQuery()
                .processInstanceId(processInstance.getId())
                .orderByTaskName().asc()
                .list();
        Assert.assertEquals(2, tasks.size());
        Assert.assertEquals("Financial negotiation", tasks.get(0).getName());
        Assert.assertEquals("Tech interview", tasks.get(1).getName());

        // Completing both should wrap up the subprocess, send out the 'welcome mail' and end the process instance
        taskVariables = new HashMap<String, Object>();
        taskVariables.put("techOk", true);
        taskService.complete(tasks.get(0).getId(), taskVariables);

        taskVariables = new HashMap<String, Object>();
        taskVariables.put("financialOk", true);
        taskService.complete(tasks.get(1).getId(), taskVariables);

        // Verify email
        Assert.assertEquals(1, wiser.getMessages().size());

        // Verify process completed
        Assert.assertEquals(1, historyService.createHistoricProcessInstanceQuery().finished().count());

    }
}
```

## Next steps

* We haven�t touched any of the tooling around camunda. There is a bunch more than just the engine, like the Eclipse plugin to design processes, a free web editor in the cloud (also included in the `.zip` download you can get from [camunda's site](http://camunda.org/), a web application that showcases many of the features of the engine, �
* The current release of camunda (version 5.17.0) has integration with Spring Boot 1.1.6. However, the current master version is compatible with 1.2.1.
* Using Spring Boot 1.2.0 brings us sweet stuff like support for XA transactions with JTA. This means you can hook up your processes easily with JMS, JPA and camunda logic all in the same transaction! ..Which brings us to the next point �
* In this example, we�ve focussed heavily on human interactions (and barely touched it). But there�s many things you can do around orchestrating systems too. The Spring Boot integration also has Spring Integration support you could leverage to do just that in a very neat way!
* And of course there is much much more about the BPMN 2.0 standard. Read more about it [in the camunda docs](http://camunda.org/userguide/index.html#bpmnConstructs).

## Addenda
These addenda are not a part of the original article.

### Spring Boot Maven plugin
The [Spring Boot Maven plugin](http://docs.spring.io/spring-boot/docs/current/maven-plugin/index.html) can be used directly with this example application. It can make it easier to run the Spring Boot app with a simple `mvn spring-boot:run`. Here is the POM file configuration needed:

```maven
<build>
  ...
  <plugins>
    ...
    <plugin>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-maven-plugin</artifactId>
      <version>1.2.2.RELEASE</version>
      <executions>
        <execution>
          <goals>
            <goal>repackage</goal>
          </goals>
        </execution>
      </executions>
    </plugin>
    ...
  </plugins>
  ...
</build>
```

See [the usage page](http://docs.spring.io/spring-boot/docs/current/maven-plugin/usage.html) for full documentation.

### Gradle
Given that this is a Spring Boot application, everything done here in Maven can be easily done (often more easily than in Maven) using [Gradle](http://gradle.org/). See [the Gradle plugin documentation](http://docs.spring.io/spring-boot/docs/current/reference/html/build-tool-plugins-gradle-plugin.html) for Spring Boot for more details.

### Article author's blog

[Joram Barrez's blog](http://www.jorambarrez.be/blog/)