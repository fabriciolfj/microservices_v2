package com.github.microservices.composite.product.services;

import java.util.List;
import java.util.logging.Level;
import java.util.stream.Collectors;

import com.github.api.composite.product.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;
import com.github.api.core.product.Product;
import com.github.api.core.recommendation.Recommendation;
import com.github.api.core.review.Review;
import com.github.util.http.ServiceUtil;
import reactor.core.publisher.Mono;


@RestController
public class ProductCompositeServiceImpl implements ProductCompositeService {

    private static final Logger LOG = LoggerFactory.getLogger(ProductCompositeServiceImpl.class);

    private final ServiceUtil serviceUtil;
    private ProductCompositeIntegration integration;

    @Autowired
    public ProductCompositeServiceImpl(final ServiceUtil serviceUtil, final ProductCompositeIntegration integration) {
        this.serviceUtil = serviceUtil;
        this.integration = integration;
    }

    @Override
    public Mono<Void> createProduct(ProductAggregate body) {
        try {
            LOG.debug("createCompositeProduct: creates a new composite entity for productId: {}", body.getProductId());

            Product product = new Product(body.getProductId(), body.getName(), body.getWeight(), null);
            integration.createProduct(product);

            if (body.getRecommendations() != null) {
                body.getRecommendations().forEach(r -> {
                    Recommendation recommendation = new Recommendation(body.getProductId(), r.getRecommendationId(), r.getAuthor(), r.getRate(), r.getContent(), null);
                    integration.createRecommendation(recommendation);
                });
            }

            if (body.getReviews() != null) {
                body.getReviews().forEach(r -> {
                    Review review = new Review(body.getProductId(), r.getReviewId(), r.getAuthor(), r.getSubject(), r.getContent(), null);
                    integration.createReview(review);
                });
            }

            LOG.debug("createCompositeProduct: composite entities created for productId: {}", body.getProductId());
            return Mono.empty();
        } catch (RuntimeException re) {
            LOG.warn("createCompositeProduct failed", re);
            throw re;
        }
    }

    @Override
    public Mono<ProductAggregate> getProduct(int productId) {
        return Mono.zip(
                values -> createProductAggregate((Product) values[0],
                        (List<Recommendation>) values[1],
                        (List<Review>) values[2],
                        serviceUtil.getServiceAddress()),
                integration.getProduct(productId),
                integration.getRecommendations(productId).collectList(),
                integration.getReviews(productId).collectList()
                        .doOnError(ex -> LOG.warn("getCompositeProduct failed: {}", ex.toString()))
                        .log(LOG.getName(), Level.FINE));
    }

    @Override
    public Mono<Void> deleteProduct(int productId) {
        return Mono.zip(
                        r -> "",
                        integration.deleteProduct(productId),
                        integration.deleteReviews(productId),
                        integration.deleteRecommendations(productId))
                .doOnError(e -> LOG.warn("delete failed: {}", e.getMessage()))
                .log(LOG.getName(), Level.FINE)
                .then();
    }

    private ProductAggregate createProductAggregate(
            final Product product,
            final List<Recommendation> recommendations,
            final List<Review> reviews,
            final String serviceAddress) {

        // 1. Setup product info
        int productId = product.getProductId();
        String name = product.getName();
        int weight = product.getWeight();

        // 2. Copy summary recommendation info, if available
        final List<RecommendationSummary> recommendationSummaries = (recommendations == null) ? null :
                recommendations.stream()
                        .map(r -> new RecommendationSummary(r.getRecommendationId(), r.getAuthor(), r.getRate(), r.getContent()))
                        .collect(Collectors.toList());

        // 3. Copy summary review info, if available
        final List<ReviewSummary> reviewSummaries = (reviews == null) ? null :
                reviews.stream()
                        .map(r -> new ReviewSummary(r.getReviewId(), r.getAuthor(), r.getSubject(), r.getContent()))
                        .collect(Collectors.toList());

        // 4. Create info regarding the involved microservices addresses
        final String productAddress = product.getServiceAddress();
        final String reviewAddress = (reviews != null && reviews.size() > 0) ? reviews.get(0).getServiceAddress() : "";
        final String recommendationAddress = (recommendations != null && recommendations.size() > 0) ? recommendations.get(0).getServiceAddress() : "";
        final ServiceAddresses serviceAddresses = new ServiceAddresses(serviceAddress, productAddress, reviewAddress, recommendationAddress);

        return new ProductAggregate(productId, name, weight, recommendationSummaries, reviewSummaries, serviceAddresses);
    }
}
