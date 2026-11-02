package com.opencirc.api.passport.dao;

import com.opencirc.api.passport.model.Owner;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Owner organization repository. */
@Repository
public interface OwnerRepository extends JpaRepository<Owner, String> {

  /** Lists owners in creation order. */
  List<Owner> findAllByOrderByCreatedTimeAsc();

  /** Finds an owner by its exact name. */
  Optional<Owner> findFirstByName(String name);
}
