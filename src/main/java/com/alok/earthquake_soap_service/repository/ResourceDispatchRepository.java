package com.alok.earthquake_soap_service.repository;

import com.alok.earthquake_soap_service.model.ResourceDispatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ResourceDispatchRepository extends JpaRepository<ResourceDispatch, String> {

    List<ResourceDispatch> findByEarthquakeId(String earthquakeId);

    List<ResourceDispatch> findByDestinationRegion(String destinationRegion);
}
