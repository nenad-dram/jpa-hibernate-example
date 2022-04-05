/*
 * This Java source file was generated by the Gradle 'init' task.
 */
package com.endyary.jpaexample;

import com.endyary.jpaexample.model.Customer;
import com.endyary.jpaexample.model.Order;
import com.endyary.jpaexample.model.Product;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.time.LocalDateTime;
import java.util.List;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class JpaTest {
    static EntityManagerFactory entityManagerFactory;

    @BeforeAll
    static void initEntityManagerFactory () {
        entityManagerFactory = Persistence.createEntityManagerFactory("jpa-h2");
    }

    @Test
    @org.junit.jupiter.api.Order(1)
    void insertCustomer() {
        EntityManager entityManager = entityManagerFactory.createEntityManager();

        Customer expectedCustomer = MockDataProvider.getCustomer();
        entityManager.getTransaction().begin();
        entityManager.persist(expectedCustomer);
        entityManager.getTransaction().commit();
        entityManager.clear();

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Customer> cq = cb.createQuery(Customer.class);
        Root<Customer> root = cq.from(Customer.class);
        cq.select(root).where(cb.equal(root.get("email"), expectedCustomer.getEmail()));

        Customer actualCustomer = entityManager.createQuery(cq).getSingleResult();
        entityManager.close();

        Assertions.assertEquals(expectedCustomer.getEmail(), actualCustomer.getEmail());
    }

    @Test
    @org.junit.jupiter.api.Order(2)
    void insertProducts() {
        EntityManager entityManager = entityManagerFactory.createEntityManager();

        List<Product> expectedProducts = MockDataProvider.getProductList();
        entityManager.getTransaction().begin();
        expectedProducts.forEach(entityManager::persist);
        entityManager.getTransaction().commit();
        entityManager.clear();

        long actualSize = (long) entityManager.createQuery("SELECT count(*) FROM Product").getSingleResult();
        entityManager.close();

        Assertions.assertEquals(expectedProducts.size(), actualSize);
    }

    @Test
    @org.junit.jupiter.api.Order(3)
    void insertOrder() {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        entityManager.getTransaction().begin();

        List<Product> productList = entityManager.createQuery("SELECT p FROM Product p").getResultList();
        Customer customer = entityManager.find(Customer.class, 1L);

        Order expectedOrder = MockDataProvider.getOrderWithItems(productList);
        expectedOrder.setCustomer(customer);
        entityManager.persist(expectedOrder);
        entityManager.getTransaction().commit();
        entityManager.clear();

        Order actualOrder = entityManager.find(Order.class, 1L);
        entityManager.close();

        SoftAssertions orderAssert = new SoftAssertions();
        orderAssert.assertThat(actualOrder.getStatus()).isEqualTo(expectedOrder.getStatus());
        orderAssert.assertThat(expectedOrder.getCustomer().getId()).isEqualTo(actualOrder.getCustomer().getId());
        orderAssert.assertThat(expectedOrder.getItems().get(0).getProduct().getId())
                .isEqualTo(actualOrder.getItems().get(0).getProduct().getId());
        orderAssert.assertAll();
    }

    @Test
    @org.junit.jupiter.api.Order(4)
    void updateOrderStatus() {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        entityManager.getTransaction().begin();

        Order expectedOrder = entityManager.find(Order.class, 1L);
        expectedOrder.setStatus(Order.Status.SHIPPED);
        expectedOrder.setModifiedDate(LocalDateTime.now());
        entityManager.getTransaction().commit();

        Order actualOrder = entityManager.find(Order.class, 1L);

        SoftAssertions orderAssert = new SoftAssertions();
        orderAssert.assertThat(Order.Status.SHIPPED).isEqualTo(actualOrder.getStatus());
        orderAssert.assertThat(expectedOrder.getModifiedDate()).isNotNull();
        orderAssert.assertAll();
    }

    @Test
    @org.junit.jupiter.api.Order(5)
    void deleteUnavailableProduct() {
        EntityManager entityManager = entityManagerFactory.createEntityManager();

        List<Product> initialList = MockDataProvider.getProductList();

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Product> cq = cb.createQuery(Product.class);
        Root<Product> root = cq.from(Product.class);
        cq.select(root).where(cb.equal(root.get("status"), Product.Status.UNAVAILABLE));
        List<Product> resultList = entityManager.createQuery(cq).getResultList();

        entityManager.getTransaction().begin();
        resultList.forEach(entityManager::remove);
        entityManager.getTransaction().commit();
        entityManager.clear();

        long actualSize = (long) entityManager.createQuery("SELECT count(*) FROM Product").getSingleResult();
        entityManager.close();

        Assertions.assertEquals(initialList.size() - resultList.size(), actualSize);
    }
}