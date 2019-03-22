package edu.mayo.kmdp.repository.artifact;

public class ResourceNotFoundException extends RuntimeException {

  public ResourceNotFoundException(String id) {
    super("Resource with ID: " + id + " not found.");
  }

}
