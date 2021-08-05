package com.github.microservices.core.review.services;

import java.util.List;

import com.github.microservices.core.review.persistence.ReviewRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.bind.annotation.RestController;
import com.github.api.core.review.Review;
import com.github.api.core.review.ReviewService;
import com.github.api.exceptions.InvalidInputException;
import com.github.microservices.core.review.persistence.ReviewEntity;
import com.github.util.http.ServiceUtil;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
public class ReviewServiceImpl implements ReviewService {

  private static final Logger LOG = LoggerFactory.getLogger(ReviewServiceImpl.class);

  private final ReviewRepository repository;

  private final ReviewMapper mapper;

  private final ServiceUtil serviceUtil;

  @Autowired
  public ReviewServiceImpl(ReviewRepository repository, ReviewMapper mapper, ServiceUtil serviceUtil) {
    this.repository = repository;
    this.mapper = mapper;
    this.serviceUtil = serviceUtil;
  }

  @Override
  public Mono<Review> createReview(Review body) {
    try {
      ReviewEntity entity = mapper.apiToEntity(body);
      ReviewEntity newEntity = repository.save(entity);

      LOG.debug("createReview: created a review entity: {}/{}", body.getProductId(), body.getReviewId());
      return mapper.entityToApi(newEntity);

    } catch (DataIntegrityViolationException dive) {
      throw new InvalidInputException("Duplicate key, Product Id: " + body.getProductId() + ", Review Id:" + body.getReviewId());
    }
  }

  @Override
  public Flux<Review> getReviews(int productId) {

    if (productId < 1) {
      throw new InvalidInputException("Invalid productId: " + productId);
    }
    
    List<ReviewEntity> entityList = repository.findByProductId(productId);
    List<Review> list = mapper.entityListToApiList(entityList);
    list.forEach(e -> e.setServiceAddress(serviceUtil.getServiceAddress()));

    LOG.debug("getReviews: response size: {}", list.size());

    return list;
  }

  @Override
  public Mono<Void> deleteReviews(int productId) {
    LOG.debug("deleteReviews: tries to delete reviews for the product with productId: {}", productId);
    repository.deleteAll(repository.findByProductId(productId));
  }
}