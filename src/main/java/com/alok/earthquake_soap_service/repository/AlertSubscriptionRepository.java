package com.alok.earthquake_soap_service.repository;

import com.alok.earthquake_soap_service.model.AlertSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AlertSubscriptionRepository extends JpaRepository<AlertSubscription, String> {

    List<AlertSubscription> findByActiveTrue();

    List<AlertSubscription> findBySubscriberContact(String contact);
}
