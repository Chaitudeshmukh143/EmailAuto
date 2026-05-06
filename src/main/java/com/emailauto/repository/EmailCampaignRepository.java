package com.emailauto.repository;

import com.emailauto.domain.EmailCampaign;
import com.emailauto.domain.CampaignStatus;
import com.emailauto.domain.UserAccount;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EmailCampaignRepository extends JpaRepository<EmailCampaign, Long> {
    List<EmailCampaign> findTop10ByUserOrderByCreatedAtDesc(UserAccount user);

    @Query("select c from EmailCampaign c left join fetch c.attachments where c.status = :status and c.scheduledAt <= :scheduledAt order by c.scheduledAt asc")
    List<EmailCampaign> findDueCampaigns(@Param("status") CampaignStatus status, @Param("scheduledAt") Instant scheduledAt);
}
