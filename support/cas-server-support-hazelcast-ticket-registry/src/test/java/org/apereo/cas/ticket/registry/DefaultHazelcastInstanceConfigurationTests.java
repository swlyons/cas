package org.apereo.cas.ticket.registry;

import org.apereo.cas.config.CasCoreAuthenticationConfiguration;
import org.apereo.cas.config.CasCoreAuthenticationHandlersConfiguration;
import org.apereo.cas.config.CasCoreAuthenticationMetadataConfiguration;
import org.apereo.cas.config.CasCoreAuthenticationPolicyConfiguration;
import org.apereo.cas.config.CasCoreAuthenticationPrincipalConfiguration;
import org.apereo.cas.config.CasCoreAuthenticationServiceSelectionStrategyConfiguration;
import org.apereo.cas.config.CasCoreAuthenticationSupportConfiguration;
import org.apereo.cas.config.CasCoreConfiguration;
import org.apereo.cas.config.CasCoreHttpConfiguration;
import org.apereo.cas.config.CasCoreServicesAuthenticationConfiguration;
import org.apereo.cas.config.CasCoreServicesConfiguration;
import org.apereo.cas.config.CasCoreTicketCatalogConfiguration;
import org.apereo.cas.config.CasCoreTicketsConfiguration;
import org.apereo.cas.config.CasCoreUtilConfiguration;
import org.apereo.cas.config.CasCoreWebConfiguration;
import org.apereo.cas.config.CasHazelcastConfiguration;
import org.apereo.cas.config.CasPersonDirectoryConfiguration;
import org.apereo.cas.config.HazelcastTicketRegistryConfiguration;
import org.apereo.cas.config.HazelcastTicketRegistryTicketCatalogConfiguration;
import org.apereo.cas.config.support.CasWebApplicationServiceFactoryConfiguration;
import org.apereo.cas.config.support.EnvironmentConversionServiceInitializer;
import org.apereo.cas.logout.config.CasCoreLogoutConfiguration;
import org.apereo.cas.util.SchedulingUtils;

import com.hazelcast.core.HazelcastInstance;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.junit.After;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cloud.autoconfigure.RefreshAutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import java.util.Arrays;

import static org.junit.Assert.*;

/**
 * @author Dmitriy Kopylenko
 * @since 4.2.0
 */
@SpringBootTest(classes = {
    DefaultHazelcastInstanceConfigurationTests.HazelcastTestConfiguration.class,
    CasHazelcastConfiguration.class,
    HazelcastTicketRegistryConfiguration.class,
    CasCoreTicketsConfiguration.class,
    RefreshAutoConfiguration.class,
    CasCoreTicketCatalogConfiguration.class,
    HazelcastTicketRegistryTicketCatalogConfiguration.class,
    CasCoreTicketCatalogConfiguration.class,
    CasCoreUtilConfiguration.class,
    CasPersonDirectoryConfiguration.class,
    CasCoreLogoutConfiguration.class,
    CasCoreAuthenticationConfiguration.class,
    CasCoreServicesAuthenticationConfiguration.class,
    CasCoreAuthenticationPrincipalConfiguration.class,
    CasCoreAuthenticationPolicyConfiguration.class,
    CasCoreAuthenticationMetadataConfiguration.class,
    CasCoreAuthenticationSupportConfiguration.class,
    CasCoreAuthenticationHandlersConfiguration.class,
    CasCoreHttpConfiguration.class,
    CasCoreConfiguration.class,
    CasCoreAuthenticationServiceSelectionStrategyConfiguration.class,
    CasCoreServicesConfiguration.class,
    CasCoreLogoutConfiguration.class,
    CasCoreWebConfiguration.class,
    CasWebApplicationServiceFactoryConfiguration.class})
@ContextConfiguration(initializers = EnvironmentConversionServiceInitializer.class)
@DirtiesContext
@Slf4j
public class DefaultHazelcastInstanceConfigurationTests {
    @ClassRule
    public static final SpringClassRule SPRING_CLASS_RULE = new SpringClassRule();

    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();

    @Autowired
    @Qualifier("casHazelcastInstance")
    private HazelcastInstance hzInstance;

    public HazelcastInstance getHzInstance() {
        return hzInstance;
    }

    @Test
    public void correctHazelcastInstanceIsCreated() {
        assertNotNull(this.hzInstance);
        val config = this.hzInstance.getConfig();
        assertFalse(config.getNetworkConfig().getJoin().getMulticastConfig().isEnabled());
        assertEquals(Arrays.asList("localhost"), config.getNetworkConfig().getJoin().getTcpIpConfig().getMembers());
        assertTrue(config.getNetworkConfig().isPortAutoIncrement());
        assertEquals(5701, config.getNetworkConfig().getPort());
        assertEquals(5, config.getMapConfigs().size());
    }

    @After
    public void shutdownHz() {
        LOGGER.info("Shutting down hazelcast instance [{}]", this.hzInstance.getConfig().getInstanceName());
        this.hzInstance.shutdown();
        while (this.hzInstance.getLifecycleService().isRunning()) {
            LOGGER.info("Waiting for instances to shut down");
        }
    }

    @TestConfiguration
    public static class HazelcastTestConfiguration implements InitializingBean {
        @Autowired
        protected ApplicationContext applicationContext;

        @Override
        public void afterPropertiesSet() {
            SchedulingUtils.prepScheduledAnnotationBeanPostProcessor(applicationContext);
        }
    }
}