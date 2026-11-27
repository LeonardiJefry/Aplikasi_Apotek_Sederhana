package id.ac.ub.apotek.demo;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile({"demo", "test", "mysql", "supabase"})
@ConditionalOnProperty(name = "klinik.demo.enabled", havingValue = "true")
public class DemoDataInitializer implements ApplicationRunner {
    private final DemoDataService data;
    public DemoDataInitializer(DemoDataService data) { this.data = data; }
    @Override public void run(ApplicationArguments args) { data.seed(); }
}
