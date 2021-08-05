package com.github.microservices.core.product.services;

import com.github.microservices.core.product.persistence.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.web.bind.annotation.RestController;
import com.github.api.core.product.Product;
import com.github.api.core.product.ProductService;
import com.github.api.exceptions.InvalidInputException;
import com.github.api.exceptions.NotFoundException;
import com.github.microservices.core.product.persistence.ProductEntity;
import com.github.util.http.ServiceUtil;
import reactor.core.publisher.Mono;

import java.util.logging.Level;

@RestController
public class ProductServiceImpl implements ProductService {

  private static final Logger LOG = LoggerFactory.getLogger(ProductServiceImpl.class);

  private final ServiceUtil serviceUtil;

  private final ProductRepository repository;

  private final ProductMapper mapper;

  @Autowired
  public ProductServiceImpl(ProductRepository repository, ProductMapper mapper, ServiceUtil serviceUtil) {
    this.repository = repository;
    this.mapper = mapper;
    this.serviceUtil = serviceUtil;
  }

  @Override
  public Mono<Product> createProduct(Product body) {
    checkIdProduct(body.getProductId());

    return Mono.just(mapper.apiToEntity(body))
            .flatMap(repository::save)
            .log(LOG.getName(), Level.FINE)
            .onErrorMap(DuplicateKeyException.class,
                    ex -> new InvalidInputException("Duplicate key, Product Id: " + body.getProductId()))
            .map(mapper::entityToApi);
  }

  @Override
  public Mono<Product> getProduct(int productId) {
    checkIdProduct(productId);

    return repository.findByProductId(productId)
            .log(LOG.getName(), Level.FINE)
            .map(mapper::entityToApi)
            .map(dto ->  {
              dto.setServiceAddress(serviceUtil.getServiceAddress());
              return dto;
            })
            .switchIfEmpty(Mono.defer(() -> Mono.error(new NotFoundException("No product found for productId: " + productId) )));
  }

  @Override
  public Mono<Void> deleteProduct(int productId) {
    checkIdProduct(productId);

    LOG.debug("deleteProduct: tries to delete an entity with productId: {}", productId);
    return repository.findByProductId(productId).map(e -> repository.delete(e)).flatMap( e-> e);
  }

  private void checkIdProduct(int productId) {
    if (productId < 1) {
      throw new InvalidInputException("Invalid productId: " + productId);
    }
  }
}
