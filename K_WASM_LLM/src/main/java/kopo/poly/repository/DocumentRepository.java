package kopo.poly.repository;

import kopo.poly.domain.Documents;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface DocumentRepository extends MongoRepository<Documents, String> {


}
