package com.emailauto.repository;

import com.emailauto.domain.EmailCampaign;
import com.emailauto.domain.UserAccount;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmailCampaignRepository extends JpaRepository<EmailCampaign, Long> {
    List<EmailCampaign> findTop10ByUserOrderByCreatedAtDesc(UserAccount user);
}
