package com.hanogi.batch.repositry;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import com.hanogi.batch.entity.WorldCountry;

public interface WorldCountryRepo extends CrudRepository<WorldCountry, String> {
	
	public static final String Country_Query = "Select name from world_country ORDER By name";

	@Query(value = Country_Query, nativeQuery = true)
	List<String> getCountryNames();
	
	@Override
	List<WorldCountry> findAll();
}
