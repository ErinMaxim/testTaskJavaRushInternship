package com.game.controller;

import com.game.dto.FilterDTO;
import com.game.dto.PageFilterDTO;
import com.game.entity.Player;
import com.game.entity.Profession;
import com.game.entity.Race;
import com.game.repository.PlayerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import javax.persistence.criteria.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.game.dto.FilterDTO.*;
import static com.game.dto.FilterDTO.START_OF_YEAR_2000_IN_MILLISECONDS;

@RestController
public class PlayerController {
    public PlayerRepository repository;

    @Autowired
    public PlayerController(PlayerRepository repository) {
        this.repository = repository;
    }

    @GetMapping(value = "/rest/players")
    public List<Player> players(@RequestParam(required = false) String name,
                                @RequestParam(required = false) String title,
                                @RequestParam(required = false) Race race,
                                @RequestParam(required = false) Profession profession,
                                @RequestParam(required = false) Long after,
                                @RequestParam(required = false) Long before,
                                @RequestParam(required = false) Boolean banned,
                                @RequestParam(required = false) Integer minExperience,
                                @RequestParam(required = false) Integer maxExperience,
                                @RequestParam(required = false) Integer minLevel,
                                @RequestParam(required = false) Integer maxLevel,
                                @RequestParam(required = false) PlayerOrder order,
                                @RequestParam(required = false) Integer pageNumber,
                                @RequestParam(required = false) Integer pageSize) {

        PageFilterDTO filter = new PageFilterDTO(name, title, race, profession, after, before, banned, minExperience, maxExperience,
                minLevel, maxLevel, order, pageNumber, pageSize);

        Specification<Player> spec = specificationByFilter(filter);

        if (order == null) {
            order = PlayerOrder.ID;
        }
        Sort sort = Sort.by(order.getFieldName()).ascending();
        if (pageNumber == null) {
            pageNumber = 0;
        }
        if (pageSize == null) {
            pageSize = 3;
        }
        Pageable pageable = PageRequest.of(pageNumber, pageSize, sort);
        Page<Player> all = repository.findAll(spec, pageable);
        return all.stream().collect(Collectors.toList());
    }

    @GetMapping(value = "/rest/players/count")
    public long count(@RequestParam(required = false) String name,
                      @RequestParam(required = false) String title,
                      @RequestParam(required = false) Race race,
                      @RequestParam(required = false) Profession profession,
                      @RequestParam(required = false) Long after,
                      @RequestParam(required = false) Long before,
                      @RequestParam(required = false) Boolean banned,
                      @RequestParam(required = false) Integer minExperience,
                      @RequestParam(required = false) Integer maxExperience,
                      @RequestParam(required = false) Integer minLevel,
                      @RequestParam(required = false) Integer maxLevel) {
        FilterDTO filter = new FilterDTO(name, title, race, profession, after, before, banned, minExperience, maxExperience,
                minLevel, maxLevel);

        if (!filter.hasBody()) {
            return repository.count();
        }
        Specification<Player> spec = specificationByFilter(filter);
        List<Player> all = repository.findAll(spec);
        return all.size();
    }

    @GetMapping(value = "/rest/players/{id}")
    public ResponseEntity<Player> getById(@PathVariable("id") Long id) {
        if (id <= 0) {
            return new ResponseEntity<Player>(null, new HttpHeaders(), HttpStatus.BAD_REQUEST);
        }
        Optional<Player> optional = repository.findById(id);
        if (!optional.isPresent()) {
            return new ResponseEntity<Player>(null, new HttpHeaders(), HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<Player>(optional.get(), new HttpHeaders(), HttpStatus.OK);
    }

    @DeleteMapping(value = "/rest/players/{id}")
    public ResponseEntity deleteById(@PathVariable("id") Long id) {
        if (id <= 0) {
            return new ResponseEntity(HttpStatus.BAD_REQUEST);
        }
        Optional<Player> optional = repository.findById(id);
        if (!optional.isPresent()) {
            return new ResponseEntity(HttpStatus.NOT_FOUND);
        }
        repository.deleteById(id);
        return new ResponseEntity(HttpStatus.OK);
    }

    @PostMapping(value = "/rest/players/{id}")
    public ResponseEntity<Player> update(@PathVariable("id") Long id,
                                         @RequestBody Player playerJSON) {

        if (id <= 0) {
            return badRequest();
        }
        Optional<Player> optional = repository.findById(id);
        if (!optional.isPresent()) {
            return notFound();
        }
        Player player = optional.get();

        String name = playerJSON.getName();
        if (name != null) {
            if (name.length() == 0 || name.length() > 12) {
                return badRequest();
            }
            player.setName(name);
        }
        String title = playerJSON.getTitle();
        if (title != null) {
            if (title.length() > 30 || title.length() == 0) {
                return badRequest();
            }
            player.setTitle(title);

        }
        if (playerJSON.getRace() != null) {
            player.setRace(playerJSON.getRace());

        }
        if (playerJSON.getProfession() != null) {
            player.setProfession(playerJSON.getProfession());

        }
        Date birthday = playerJSON.getBirthday();
        if (birthday != null) {
            if (!isInInterval(birthday.getTime(), START_OF_YEAR_2000_IN_MILLISECONDS, END_OF_YEAR_3000_IN_MILLISECONDS)) {
                return badRequest();
            }
            player.setBirthday(birthday.getTime());

        }
        if (playerJSON.getBanned() != null) {
            player.setBanned(playerJSON.getBanned());

        }
        Integer experience = playerJSON.getExperience();
        if (experience != null) {
            if (!isInInterval((long) experience, 0L, 10_000_000L)) {
                return badRequest();
            }
            player.setExperience(experience);
        }

        player.calculateExp();
        Player save = repository.save(player);
        return new ResponseEntity<Player>(save, new HttpHeaders(), HttpStatus.OK);
    }

    @PostMapping(value = "/rest/players")
    public ResponseEntity<Player> create(@RequestBody Player playerJSON) {
        if (!hasEnoughParamsToCreate(playerJSON)) {
            return badRequest();
        }
        Player player = new Player();

        String name = playerJSON.getName();
        if (name.length() == 0 || name.length() > 12) {
            return badRequest();
        }
        player.setName(name);

        String title = playerJSON.getTitle();
        if (title.length() > 30 || title.length() == 0) {
            return badRequest();
        }
        player.setTitle(title);

        player.setRace(playerJSON.getRace());
        player.setProfession(playerJSON.getProfession());

        Date birthday = playerJSON.getBirthday();
        if (!isInInterval(birthday.getTime(), START_OF_YEAR_2000_IN_MILLISECONDS, END_OF_YEAR_3000_IN_MILLISECONDS)) {
            return badRequest();
        }
        player.setBirthday(birthday.getTime());

        Boolean banned = playerJSON.getBanned();
        if (banned != null) {
            player.setBanned(banned);
        } else {
            player.setBanned(false);
        }

        Integer experience = playerJSON.getExperience();
        if (!isInInterval((long) experience, 0L, 10_000_000L)) {
            return badRequest();
        }
        player.setExperience(experience);

        player.calculateExp();
        Player save = repository.save(player);
        return new ResponseEntity<Player>(save, new HttpHeaders(), HttpStatus.OK);
    }

    private boolean hasEnoughParamsToCreate(Player player) {
        return player.getName() != null && player.getBirthday() != null && player.getExperience() != null
                && player.getProfession() != null && player.getRace() != null && player.getTitle() != null;
    }

    private ResponseEntity badRequest() {
        return new ResponseEntity<Player>(null, new HttpHeaders(), HttpStatus.BAD_REQUEST);
    }

    private ResponseEntity notFound() {
        return new ResponseEntity<Player>(null, new HttpHeaders(), HttpStatus.NOT_FOUND);
    }

    private static Specification<Player> specificationByFilter(FilterDTO filter) {
        return new Specification<Player>() {
            @Override
            public Predicate toPredicate(Root<Player> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {
                List<Predicate> predicates = new ArrayList<>();
                processPredicates(filter, predicates, root, query, criteriaBuilder);
                Predicate predicate = criteriaBuilder.and(predicates.toArray(new Predicate[0]));
                return predicate;
            }
        };
    }

    private static void processPredicates(FilterDTO filter, List<Predicate> predicates, Root<Player> root,
                                   CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {
        if (filter.hasName()) {
            Predicate namePredicate = criteriaBuilder.like(root.get("name"), "%" + filter.getName() + "%");
            predicates.add(namePredicate);
        }
        if (filter.hasTitle()) {
            Predicate titlePredicate = criteriaBuilder.like(root.get("title"), "%" + filter.getTitle() + "%");
            predicates.add(titlePredicate);
        }
        if (filter.hasRace()) {
            Predicate racePredicate = criteriaBuilder.equal(root.get("race"), filter.getRace());
            predicates.add(racePredicate);
        }
        if (filter.hasProfession()) {
            Predicate professionPredicate = criteriaBuilder.equal(root.get("profession"), filter.getProfession());
            predicates.add(professionPredicate);
        }
        if (filter.hasBanned()) {
            Predicate bannedPredicate = criteriaBuilder.equal(root.get("banned"), filter.getBanned());
            predicates.add(bannedPredicate);
        }

        Predicate afterPredicate = criteriaBuilder.greaterThanOrEqualTo(root.<Date>get("birthday"), filter.getAfter());
        Predicate beforePredicate = criteriaBuilder.lessThanOrEqualTo(root.<Date>get("birthday"), filter.getBefore());
        if (filter.hasAfter() && filter.hasBefore()) {
            if (filter.isBeforeMoreThanAfter()) {
                predicates.add(afterPredicate);
                predicates.add(beforePredicate);
            }
        } else {
            if (filter.hasAfter()) {
                predicates.add(afterPredicate);
            }
            if (filter.hasBefore()) {
                predicates.add(beforePredicate);
            }
        }

        Predicate minExperiencePredicate = criteriaBuilder.ge(root.get("experience"), filter.getMinExperience());
        Predicate maxExperiencePredicate = criteriaBuilder.le(root.get("experience"), filter.getMaxExperience());
        if (filter.hasMinExperience() && filter.hasMaxExperience()) {
            if (filter.isMaxMoreThanMinExperience()) {
                predicates.add(minExperiencePredicate);
                predicates.add(maxExperiencePredicate);
            }
        } else {
            if (filter.hasMinExperience()) {
                predicates.add(minExperiencePredicate);
            }
            if (filter.hasMaxExperience()) {
                predicates.add(maxExperiencePredicate);
            }
        }

        Predicate minLevelPredicate = criteriaBuilder.ge(root.get("level"), filter.getMinLevel());
        Predicate maxLevelPredicate = criteriaBuilder.le(root.get("level"), filter.getMaxLevel());
        if (filter.hasMinLevel() && filter.hasMaxLevel()) {
            if (filter.isMaxLevelMoreThanMinLevel()) {
                predicates.add(minLevelPredicate);
                predicates.add(maxLevelPredicate);
            }
        } else {
            if (filter.hasMinLevel()) {
                predicates.add(minLevelPredicate);
            }
            if (filter.hasMaxLevel()) {
                predicates.add(maxLevelPredicate);
            }
        }
    }
}
