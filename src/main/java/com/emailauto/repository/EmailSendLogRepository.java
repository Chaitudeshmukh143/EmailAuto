package com.emailauto.repository;

import com.emailauto.domain.EmailSendLog;
import com.emailauto.domain.UserAccount;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmailSendLogRepository extends JpaRepository<EmailSendLog, Long> {
    long countByCampaignUserAndSuccess(UserAccount user, boolean success);
    List<EmailSendLog> findTop25ByCampaignUserAndSuccessFalseOrderByCreatedAtDesc(UserAccount user);
}
