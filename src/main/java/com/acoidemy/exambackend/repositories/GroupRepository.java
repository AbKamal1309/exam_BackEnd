package com.acoidemy.exambackend.repositories;


import com.acoidemy.exambackend.entities.AppUser;
import com.acoidemy.exambackend.entities.Group;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface GroupRepository extends JpaRepository<Group, Long> {

    // Trouver tous les groupes créés par un utilisateur
    List<Group> findByCreator(AppUser creator);
    List<Group> findByCreatorId(Long creatorId);

    // Trouver tous les groupes dont un utilisateur est membre
    @Query("SELECT g FROM Group g JOIN g.members m WHERE m.id = :userId")
    List<Group> findGroupsByMemberId(@Param("userId") Long userId);

    // Trouver tous les groupes dont un utilisateur est admin
    @Query("SELECT g FROM Group g JOIN g.admins a WHERE a.id = :userId")
    List<Group> findGroupsByAdminId(@Param("userId") Long userId);

    // Vérifier si un groupe existe par son nom
    Optional<Group> findByGroupName(String groupName);
    boolean existsByGroupName(String groupName);

    // Chercher les groupes par nom (recherche partielle)
  //  List<Group> findByNameContainingIgnoreCase(String name);

    // Dans GroupRepository.java
    List<Group> findByVisibility(String visibility);
    List<Group> findByGroupNameContainingIgnoreCaseOrGroupDescriptionContainingIgnoreCase(String nameKeyword, String descKeyword);
}
