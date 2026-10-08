package com.wbscouting.api.repository;

import com.wbscouting.api.entity.ContactChannel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ContactChannelRepository extends JpaRepository<ContactChannel, UUID> {

    List<ContactChannel> findByActiveTrueOrderByDisplayOrderAsc();

    List<ContactChannel> findAllByOrderByDisplayOrderAsc();

    List<ContactChannel> findByType(String type);
}
