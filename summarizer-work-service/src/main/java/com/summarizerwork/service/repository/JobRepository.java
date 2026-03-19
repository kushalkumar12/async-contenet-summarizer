package com.summarizerwork.service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.summarizerwork.service.model.Job;

@Repository
public interface JobRepository extends JpaRepository<Job, String> {

}
