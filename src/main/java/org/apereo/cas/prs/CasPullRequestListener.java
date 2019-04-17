package org.apereo.cas.prs;

import org.apereo.cas.CasLabels;
import org.apereo.cas.MonitoredRepository;
import org.apereo.cas.PullRequestListener;
import org.apereo.cas.github.Milestone;
import org.apereo.cas.github.PullRequest;
import org.apereo.cas.github.PullRequestFile;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
public class CasPullRequestListener implements PullRequestListener {
    private final MonitoredRepository repository;

    @Override
    public void onOpenPullRequest(final PullRequest pr) {
        log.debug("Processing {}", pr);

        if (processLabelSeeMaintenancePolicy(pr)) {
            return;
        }

        processLabelPendingPortForward(pr);
        processLabelPendingUpdateProperty(pr);
        processMilestoneAssignment(pr);
        processLabelsByFeatures(pr);

        removeLabelWorkInProgress(pr);
    }

    private void removeLabelWorkInProgress(final PullRequest pr) {
        if (pr.isLabeledAs(CasLabels.LABEL_WIP)) {
            val title = pr.getTitle().toLowerCase();
            if (CasLabels.LABEL_WIP.getKeywords() != null && !CasLabels.LABEL_WIP.getKeywords().matcher(title).find()) {
                log.info("{} will remove the label {}", pr, CasLabels.LABEL_WIP);
                repository.removeLabelFrom(pr, CasLabels.LABEL_WIP);
            }
        }
    }

    private void processLabelPendingUpdateProperty(final PullRequest pr) {
        if (!pr.isLabeledAs(CasLabels.LABEL_PENDING_DOCUMENT_PROPERTY)) {
            Collection<PullRequestFile> files = repository.getPullRequestFiles(pr);
            boolean hasProperty = files.stream().anyMatch(f -> f.getFilename().endsWith("Properties.java"));
            if (hasProperty) {
                boolean hasNoDocs = files.stream().noneMatch(f -> f.getFilename().contains("Configuration-Properties.md"));
                if (hasNoDocs) {
                    log.info("{} changes CAS properties, yet documentation is not updated to reflect changes", pr);
                    repository.labelPullRequestAs(pr, CasLabels.LABEL_PENDING_DOCUMENT_PROPERTY);
                }
            }
        }
    }

    private boolean processLabelSeeMaintenancePolicy(final PullRequest pr) {
        if (!pr.isTargetedAtMasterBranch() && !pr.isLabeledAs(CasLabels.LABEL_SEE_MAINTENANCE_POLICY)) {
            final Optional<Milestone> milestone = repository.getMilestoneForBranch(pr.getBase().getRef());
            if (milestone.isEmpty()) {
                log.info("{} is targeted at a branch {} that is no longer maintained. See maintenance policy", pr, pr.getBase());
                repository.labelPullRequestAs(pr, CasLabels.LABEL_SEE_MAINTENANCE_POLICY);
                repository.labelPullRequestAs(pr, CasLabels.LABEL_PROPOSAL_DECLINED);
                repository.close(pr);
                return true;
            }
        }
        return false;
    }

    private void processLabelPendingPortForward(final PullRequest pr) {
        if (!pr.getBase().isRefMaster() && !pr.isLabeledAs(CasLabels.LABEL_PENDING_PORT_FORWARD)) {
            log.info("{} is targeted at a branch {} and should be ported forward to the master branch in a separate pull request.", pr, pr.getBase());
            repository.labelPullRequestAs(pr, CasLabels.LABEL_PENDING_PORT_FORWARD);
        }
    }

    private void processMilestoneAssignment(final PullRequest pr) {
        if (pr.getMilestone() == null) {
            if (pr.isTargetedAtMasterBranch()) {
                repository.getMilestoneForMaster().ifPresent(milestone -> {
                    log.info("{} will be assigned the master milestone {}", pr, milestone);
                    repository.getGitHub().setMilestone(pr, milestone);
                });
            } else {
                final Optional<Milestone> milestone = repository.getMilestoneForBranch(pr.getBase().getRef());
                milestone.ifPresent(result -> {
                    log.info("{} will be assigned the maintenance milestone {}", pr, milestone);
                    repository.getGitHub().setMilestone(pr, result);
                });
            }
        }
    }

    private void processLabelsByFeatures(final PullRequest pr) {
        var title = pr.getTitle().toLowerCase();
        Arrays.stream(CasLabels.values()).forEach(l -> {
            if (!pr.isLabeledAs(l)) {
                if (title.matches('\b' + l.getTitle().toLowerCase() + '\b')) {
                    log.info("{} will be assigned the label {}", pr, l);
                    repository.labelPullRequestAs(pr, l);
                } else if (l.getKeywords() != null && l.getKeywords().matcher(title).find()) {
                    log.info("{} will be assigned the label {} by keywords", pr, l);
                    repository.labelPullRequestAs(pr, l);
                }
            }
        });
    }
}