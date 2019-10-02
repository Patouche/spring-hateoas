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
 * @author Greg Turnquist
 */
public class ModelBuilder2 {

	private RepresentationModel<?> subResources;

	public static <T> EntityModelBuilder<T> resource(T domainObject) {
		return new EntityModelBuilder<>(domainObject);
	}

	public static <T extends RepresentationModel<T>> SingleItemModelBuilder<T> subResource(T model) {
		return new SingleItemModelBuilder<>(model);
	}

	public static <T extends RepresentationModel<T>> EmbeddedModelBuilder<T> subResource(LinkRelation relation, T model) {
		return new EmbeddedModelBuilder<>(relation, model);
	}

	public static class EntityModelBuilder<T> {

		private EntityModel<T> entityModel;

		public EntityModelBuilder(T domainObject) {
			this.entityModel = new EntityModel<>(domainObject);
		}

		public ModelBuilder2.EntityModelBuilder<T> link(Link link) {

			entityModel.add(link);
			return this;
		}

		public EntityModel<T> build() {
			return entityModel;
		}
	}

	public static class EmbeddedModelBuilder<T extends RepresentationModel<T>> {

		private final EmbeddedWrappers wrappers;
		private final Map<LinkRelation, List<T>> entityModels;
		private final List<Link> links;

		public EmbeddedModelBuilder(LinkRelation relation, T model) {

			this.wrappers = new EmbeddedWrappers(false);
			this.entityModels = new LinkedHashMap<>();
			this.links = new ArrayList<>();
			subResource(relation, model);
		}

		public EmbeddedModelBuilder<T> subResource(LinkRelation relation, T model) {

			this.entityModels.putIfAbsent(relation, new ArrayList<>());
			this.entityModels.get(relation).add(model);
			return this;
		}

		public EmbeddedModelBuilder<T> link(Link link) {
			this.links.add(link);
			return this;
		}

		public CollectionModel<EmbeddedWrapper> build() {

			return this.entityModels.keySet().stream() //
				.flatMap(linkRelation -> this.entityModels.get(linkRelation).stream() //
					.map(model -> this.wrappers.wrap(model, linkRelation)) //
					.collect(Collectors.toList()).stream()) //
				.collect(Collectors.collectingAndThen(Collectors.toList(),
					embeddedWrappers -> new CollectionModel<>(embeddedWrappers, this.links)));
		}

	}

	public static class SingleItemModelBuilder<T extends RepresentationModel<T>> {

		private T singleItemModel;

		public SingleItemModelBuilder(T singleItemModel) {
			this.singleItemModel = singleItemModel;
		}

		public MultipleItemModelBuilder<T> subResource(T itemModel) {
			return new MultipleItemModelBuilder<>(Arrays.asList(this.singleItemModel, itemModel), Collections.emptyList());
		}

		public SingleItemModelBuilder<T> link(Link link) {

			this.singleItemModel.add(link);
			return this;
		}

		public T build() {
			return this.singleItemModel;
		}
	}

	public static class MultipleItemModelBuilder<T extends RepresentationModel<T>> {

		private final List<T> models;
		private final List<Link> links;

		public MultipleItemModelBuilder(List<T> models, List<Link> links) {

			this.models = new ArrayList<>(models);
			this.links = new ArrayList<>(links);
		}

		public MultipleItemModelBuilder<T> subResource(T model) {

			this.models.add(model);
			return this;
		}

		public MultipleItemModelBuilder<T> link(Link link) {

			this.links.add(link);
			return this;
		}

		public CollectionModel<T> build() {
			return new CollectionModel<>(this.models, this.links);
		}
	}

}
