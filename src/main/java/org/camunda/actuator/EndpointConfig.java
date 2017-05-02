package org.camunda.actuator;

/**
 * Created by ransay on 5/2/2017.
 */

import org.camunda.bpm.engine.ProcessEngine;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The idea behind this module is that Spring Security could
 * talk to the {@link org.camunda.engine.IdentityService}
 * as required.
 *
 * @author Josh Long
 */
@Configuration
@ConditionalOnClass(name = "org.springframework.boot.actuate.endpoint.AbstractEndpoint")
public class EndpointConfig {

    @Bean
    public ProcessEngineEndpoint processEngineEndpoint(ProcessEngine engine) {
        return new ProcessEngineEndpoint(engine);
    }

//    @Bean
//    public ProcessEngineMvcEndpoint processEngineMvcEndpoint(
//            ProcessEngineEndpoint engineEndpoint, RepositoryService repositoryService) {
//        return new ProcessEngineMvcEndpoint(engineEndpoint, repositoryService);
//    }
}