package com.hannzer.cqb.users;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    /** email 欄位定序不分大小寫，Google 帳號大小寫不同也能對應 */
    Optional<AppUser> findByEmail(String email);
}
