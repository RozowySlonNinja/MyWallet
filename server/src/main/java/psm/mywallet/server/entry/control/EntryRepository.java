package psm.mywallet.server.entry.control;

import com.google.common.collect.ImmutableList;
import psm.mywallet.server.jpa.entity.Entry;
import psm.mywallet.server.jpa.entity.Tag;

import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Root;
import javax.transaction.Transactional;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Adrian Michalski
 */
@Stateless
@LocalBean
public class EntryRepository {

    @PersistenceContext(unitName = "mywallet-pu")
    private EntityManager em;

    public List<Entry> getAll() {
        return em.createQuery("SELECT e FROM Entry e ORDER BY e.createDate DESC", Entry.class)
                .getResultList();
    }

    public List<Entry> getByTag(String tagName) {
        return queryForTag(tagName)
                .map(tag -> tag.getEntries().stream()
                        .sorted((a, b) -> b.getCreateDate().compareTo(a.getCreateDate()))
                        .collect(Collectors.toList()))
                .map(ImmutableList::copyOf)
                .orElse(ImmutableList.of());
    }

    public Number getSumOfAllEntriesForTag(String tagName) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Entry> q = cb.createQuery(Entry.class);
        Root<Entry> entry = q.from(Entry.class);
        Join<Entry, Tag> tag = entry.join("tags");
        q.select(entry)
                .where(cb.equal(tag.get("name"), tagName));
        TypedQuery<Entry> query = em.createQuery(q);

        return query.getResultList().stream()
                .map(Entry::getValue)
                .reduce(BigDecimal::add)
                .orElse(BigDecimal.ZERO);
    }


    public Number getAccountBalance() {
        return em.createQuery("SELECT e FROM Entry e", Entry.class).getResultList().stream()
                .map(Entry::getValue)
                .reduce(BigDecimal::add)
                .orElse(BigDecimal.ZERO);
    }

    @Transactional
    public void save(Entry entry) {
        Set<Tag> requeriedTags = entry.getTags().stream()
                .map(this::requeryOrCreateTag)
                .collect(Collectors.toSet());

        entry.setTags(requeriedTags);

        em.persist(entry);
    }

    public void remove(long entryId) {
        Query query = em.createQuery("DELETE FROM Entry e WHERE e.id = " + entryId);
        query.executeUpdate();
    }

    private Optional<Tag> queryForTag(String tagName) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Tag> q = cb.createQuery(Tag.class);
        Root<Tag> tag = q.from(Tag.class);
        q.select(tag)
                .where(cb.equal(tag.get("name"), tagName));
        TypedQuery<Tag> query = em.createQuery(q);

        return query.getResultList().stream()
                .findFirst();
    }

    private Tag requeryOrCreateTag(Tag tag) {
        return queryForTag(tag.getName()).orElse(tag);
    }

}
