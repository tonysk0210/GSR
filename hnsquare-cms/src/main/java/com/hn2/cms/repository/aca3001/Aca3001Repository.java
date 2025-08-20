package com.hn2.cms.repository.aca3001;

public interface Aca3001Repository {
    Aca3001RepositoryImpl.ProfileRow findProfileByProRecId(String proRecId);
}
