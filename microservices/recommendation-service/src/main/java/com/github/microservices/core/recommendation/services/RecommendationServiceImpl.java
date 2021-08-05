package com.github.microservices.core.recommendation.services;

import java.util.logging.Level;

import com.github.microservices.core.recommendation.persistence.RecommendationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.web.bind.annotation.RestController;
import com.github.api.core.recommendation.Recommendation;
import com.github.api.core.recommendation.RecommendationService;
import com.github.api.exceptions.InvalidInputException;
import com.github.util.http.ServiceUtil;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
public class RecommendationServiceImpl implements RecommendationService {

  private static final Logger LOG = LoggerFactory.getLogger(RecommendationServiceImpl.class);

  private final RecommendationRepository repository;

  private final RecommendationMapper mapper;

  private final ServiceUtil serviceUtil;

  @Autowired
  public RecommendationServiceImpl(RecommendationRepository repository, RecommendationMapper mapper, ServiceUtil serviceUtil) {
    this.repository = repository;
    this.mapper = mapper;
    this.serviceUtil = serviceUtil;
  }

  @Override
  public Mono<Recommendation> createRecommendation(final Recommendation body) {
    checkProductId(body.getProductId());

    return Mono.just(mapper.apiToEntity(body))
            .flatMap(repository::save)
            .log(LOG.getName(), Level.FINE)
            .onErrorMap(DuplicateKeyException.class,
                    e -> new InvalidInputException("Duplicate key, Product Id: " + body.getProductId() + ", Recommendation Id:" + body.getRecommendationId()))
            .map(mapper::entityToApi);
  }

  @Override
  public Flux<Recommendation> getRecommendations(int productId) {
    checkProductId(productId);

    return repository.findByProductId(productId)
            .log(LOG.getName(), Level.FINE)
            .map(mapper::entityToApi)
            .map(e -> {
              e.setServiceAddress(serviceUtil.getServiceAddress());
              return e;
            });
  }

  @Override
  public Mono<Void> deleteRecommendations(int productId) {
    LOG.debug("deleteRecommendations: tries to delete recommendations for the product with productId: {}", productId);
    return repository.deleteAll(repository.findByProductId(productId));
  }

  private void checkProductId(int productId) {
    if (productId < 1) {
      throw new InvalidInputException("Invalid productId: " + productId);
    }
  }
}
