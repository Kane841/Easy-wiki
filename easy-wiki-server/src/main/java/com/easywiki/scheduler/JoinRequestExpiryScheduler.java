package com.easywiki.scheduler;

import com.easywiki.entity.GroupJoinRequest;
import com.easywiki.enums.JoinRequestStatus;
import com.easywiki.repository.GroupJoinRequestRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Component
public class JoinRequestExpiryScheduler {

    private static final Logger log = LoggerFactory.getLogger(JoinRequestExpiryScheduler.class);

    private final GroupJoinRequestRepository joinRequestRepository;

    public JoinRequestExpiryScheduler(GroupJoinRequestRepository joinRequestRepository) {
        this.joinRequestRepository = joinRequestRepository;
    }

    @Scheduled(cron = "0 0 2 * * ?", zone = "Asia/Shanghai")
    @Transactional
    public void expireOldJoinRequests() {
        LocalDateTime cutoff = LocalDateTime.now(ZoneId.of("Asia/Shanghai")).minusDays(30);
        var expired = joinRequestRepository.findByStatusAndCreatedAtBefore(JoinRequestStatus.PENDING, cutoff);
        for (GroupJoinRequest request : expired) {
            request.setStatus(JoinRequestStatus.EXPIRED);
            joinRequestRepository.save(request);
        }
        log.debug("Expired {} join requests older than 30 days", expired.size());
    }
}
