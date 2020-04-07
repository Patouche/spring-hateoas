/*
 * Copyright 2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.hateoas;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.hateoas.server.core.EmbeddedWrapper;
import org.springframework.hateoas.server.core.EmbeddedWrappers;

/**
 * A builder to craft hypermedia representations.
 *
 * @author Greg Turnquist
 */
public class ModelBuilder2 {

	/**
	 * Helper method to wrap a domain object as a hypermedia entity and start the builder.
	 *
	 * @param domainObject
	 * @param <T> bare
	 * @return a {@link SingleItemModelBuilder}, encapsulating a single-item model.
	 */
	public static <T> SingleItemModelBuilder<EntityModel<T>> entity(T domainObject) {
		return new SingleItemModelBuilder<>(EntityModel.of(domainObject));
	}

	/**
	 * Helper method to wrap a {@link RepresentationModel} and start the builder.
	 *
	 * @param model
	 * @param <T>
	 * @return a {@link SingleItemModelBuilder}, encapsulating the single-item model.
	 */
	public static <T extends RepresentationModel<T>> SingleItemModelBuilder<T> model(T model) {
		return new SingleItemModelBuilder<>(model);
	}

	/**
	 * Helper method to wrap a {@link RepresentationModel}, connect it to a {@link LinkRelation}, and start the builder.
	 *
	 * @param relation
	 * @param model
	 * @param <T>
	 * @return an {@link EmbeddedModelBuilder}, encapsulating a single-item model.
	 */
	public static <T extends RepresentationModel<T>> EmbeddedModelBuilder<T> subModel(LinkRelation relation, T model) {
		return new EmbeddedModelBuilder<>(relation, model);
	}

	/**
	 * Builder API that leverages {@link EmbeddedWrappers} to build up a representation where the models are tied to a
	 * specific {@link LinkRelation}.
	 *
	 * @param <T>
	 */
	public static class EmbeddedModelBuilder<T extends RepresentationModel<T>> {

		private final EmbeddedWrappers wrappers;
		private final Map<LinkRelation, List<T>> entityModels;
		private final List<Link> links;

		/**
		 * Start the builder when you don't (yet) have any data.
		 */
		public EmbeddedModelBuilder() {

			this.wrappers = new EmbeddedWrappers(false);
			this.entityModels = new LinkedHashMap<>(); // maintains the original order of entries
			this.links = new ArrayList<>();
		}

		/**
		 * Constructor when you do have data and a {@link LinkRelation} to connect it to.
		 *
		 * @param relation
		 * @param model
		 */
		public EmbeddedModelBuilder(LinkRelation relation, T model) {

			this();
			subModel(relation, model);
		}

		/**
		 * Capture a {@link RepresentationModel} and connect it to its {@link LinkRelation}.
		 *
		 * @param relation
		 * @param model
		 * @return this
		 */
		public EmbeddedModelBuilder<T> subModel(LinkRelation relation, T model) {

			this.entityModels.putIfAbsent(relation, new ArrayList<>());
			this.entityModels.get(relation).add(model);
			return this;
		}

		/**
		 * Add {@link Link}s to the model.
		 *
		 * @param link
		 * @return this
		 */
		public EmbeddedModelBuilder<T> link(Link link) {
			this.links.add(link);
			return this;
		}

		/**
		 * Transform the map of entityModels and their {@link LinkRelation}s into a collection of {@link EmbeddedWrappers}.
		 *
		 * @return a {@link CollectionModel} of {@link EmbeddedWrappers}.
		 */
		public CollectionModel<EmbeddedWrapper> build() {

			return this.entityModels.keySet().stream() //
					.flatMap(linkRelation -> this.entityModels.get(linkRelation).stream() //
							.map(model -> this.wrappers.wrap(model, linkRelation))) //
					.collect(Collectors.collectingAndThen(Collectors.toList(),
							embeddedWrappers -> CollectionModel.of(embeddedWrappers, this.links)));
		}

	}

	/**
	 * Builder API that wraps a single-item {@link RepresentationModel}.
	 *
	 * @param <T>
	 */
	public static class SingleItemModelBuilder<T extends RepresentationModel<T>> {

		private T singleItemModel;

        /**
         * Start the builder with a single {@link RepresentationModel}.
         *
         * @param singleItemModel
         */
		SingleItemModelBuilder(T singleItemModel) {
			this.singleItemModel = singleItemModel;
		}

		/**
		 * @param itemModel
		 * @return
		 */
		public MultipleItemModelBuilder<T> model(T itemModel) {
			return new MultipleItemModelBuilder<>(Arrays.asList(this.singleItemModel, itemModel), Collections.emptyList());
		}

		/**
		 * Add a {@link Link}.
		 *
		 * @param link
		 * @return
		 */
		public SingleItemModelBuilder<T> link(Link link) {

			this.singleItemModel.add(link);
			return this;
		}

		/**
		 * Convert the builder to a final {@link RepresentationModel}.
		 *
		 * @return
		 */
		public T build() {
			return this.singleItemModel;
		}
	}

	/**
	 * Builder API that wraps a collection of {@link RepresentationModel}s and related {@link Link}s.
	 *
	 * @param <T>
	 */
	public static class MultipleItemModelBuilder<T extends RepresentationModel<T>> {

		private final List<T> models;
		private final List<Link> links;

		/**
		 * Initialize with a present collection of {@link RepresentationModel}s and {@link Link}s.
		 *
		 * @param models
		 * @param links
		 */
		MultipleItemModelBuilder(List<T> models, List<Link> links) {

			this.models = new ArrayList<>(models);
			this.links = new ArrayList<>(links);
		}

		/**
		 * Add a {@link RepresentationModel}.
		 *
		 * @param model
		 * @return this
		 */
		public MultipleItemModelBuilder<T> model(T model) {

			this.models.add(model);
			return this;
		}

		/**
		 * Add a {@link Link}.
		 *
		 * @param link
		 * @return this
		 */
		public MultipleItemModelBuilder<T> link(Link link) {

			this.links.add(link);
			return this;
		}

		/**
		 * Transform the builder into a {@link CollectionModel} of {@link RepresentationModel}.
		 *
		 * @return
		 */
		public CollectionModel<T> build() {
			return CollectionModel.of(this.models, this.links);
		}
	}

}
