package personal.social;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import personal.social.enums.RoleEnum;
import personal.social.model.*;
import personal.social.repository.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@SpringBootApplication
@EnableWebSocket
public class SocialApplication implements CommandLineRunner {

    public static void main(String[] args) {
        SpringApplication.run(SocialApplication.class, args);
    }

    private final UserRepository userRepo;
    private final RolesRepository roleRepos;

    @Autowired
    public SocialApplication(
            UserRepository userRepo,
            RolesRepository roleRepos) {
        this.userRepo = userRepo;
        this.roleRepos = roleRepos;
    }

    @Override
    public void run(String... args) throws Exception {
        LocalDateTime current = LocalDateTime.now();

        // save role to db
        List<RoleEnum> roleEnumList = new ArrayList<>();
        roleEnumList.add(RoleEnum.ADMIN);
        roleEnumList.add(RoleEnum.USER);
        for (RoleEnum role : roleEnumList) {
            if (roleRepos.findByRole(role) == null) {
                roleRepos.save(new Roles(role));
            }
        }

        // add admin user
        Users existedAd = null;
        if (userRepo.findByEmail("admin@admin.com") == null) {
            Users adminUser = new Users();
            adminUser.setEmail("admin@admin.com");
            adminUser.setPassword(new BCryptPasswordEncoder().encode("12345"));
            adminUser.setFirstName("Hồ");
            adminUser.setSurname("");
            adminUser.setLastName("Việt");
            adminUser.setPhone("0000000000");
            adminUser.setDob(LocalDate.parse("2001-07-01"));
            adminUser.setCreatedAt(LocalDateTime.now());
            adminUser.setRoles(roleRepos.findByRole(RoleEnum.ADMIN));

            userRepo.save(adminUser);
        }
    }
}
